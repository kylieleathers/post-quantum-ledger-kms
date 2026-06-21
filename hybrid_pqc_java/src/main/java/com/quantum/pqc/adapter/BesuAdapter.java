package com.quantum.pqc.adapter;

import com.quantum.pqc.kms.KmsDriver;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter for a private/permissioned Besu network. Authorization is enforced
 * by PqcAccount.sol's execute() (verified via MlDsaVerifyPrecompile on every
 * validator) — relayerCredentials only pays gas, it doesn't authorize anything.
 */
public final class BesuAdapter implements ChainAdapter<BesuAdapter.Intent, BesuAdapter.PreparedTx, TransactionReceipt> {

    /** What the caller wants the PqcAccount to do. */
    public record Intent(String accountAddress, String target, BigInteger value, byte[] callData) {}

    /** Canonical payload that gets ML-DSA-signed, plus everything needed to submit it. */
    public record PreparedTx(
            String accountAddress,
            String target,
            BigInteger value,
            byte[] callData,
            BigInteger nonce,
            byte[] payloadHash
    ) {}

    private final Web3j web3j;
    private final KmsDriver kmsDriver;
    private final Credentials relayerCredentials; // pays gas only, never authorizes anything
    private final long chainId;

    public BesuAdapter(Web3j web3j, KmsDriver kmsDriver, Credentials relayerCredentials, long chainId) {
        this.web3j = web3j;
        this.kmsDriver = kmsDriver;
        this.relayerCredentials = relayerCredentials;
        this.chainId = chainId;
    }

    @Override
    public String registerIdentity(String keyId) throws Exception {
        byte[] mlDsaPublicKey = kmsDriver.getSigningPublicKey(keyId);

        // PqcAccount constructor(bytes mlDsaPublicKey) — deploy via relayer.
        String constructorParams = TypeEncoder.encode(new DynamicBytes(mlDsaPublicKey));
        String initCode = PQC_ACCOUNT_BYTECODE + constructorParams; // see note below on bytecode

        BigInteger nonce = web3j.ethGetTransactionCount(
                relayerCredentials.getAddress(), DefaultBlockParameterName.LATEST).send().getTransactionCount();

        RawTransaction deployTx = RawTransaction.createContractTransaction(
                nonce, gasPrice(), gasLimit(), BigInteger.ZERO, initCode);

        byte[] signed = TransactionEncoder.signMessage(deployTx, chainId, relayerCredentials);
        String txHash = web3j.ethSendRawTransaction(Numeric.toHexString(signed)).send().getTransactionHash();

        TransactionReceipt receipt = pollForReceipt(txHash);
        return receipt.getContractAddress();
    }

    @Override
    public PreparedTx prepareTransaction(Intent intent) throws Exception {
        BigInteger accountNonce = readAccountNonce(intent.accountAddress());

        // Must match Solidity's keccak256(abi.encode(target, value, data, nonce, address(this)))
        byte[] encoded = concat(
                encodePadded(new Address(intent.target())),
                encodePadded(new Uint256(intent.value())),
                TypeEncoder.encode(new DynamicBytes(intent.callData())).getBytes(),
                encodePadded(new Uint256(accountNonce)),
                encodePadded(new Address(intent.accountAddress()))
        );
        byte[] payloadHash = Hash.sha3(encoded);

        return new PreparedTx(intent.accountAddress(), intent.target(), intent.value(),
                intent.callData(), accountNonce, payloadHash);
    }

    @Override
    public byte[] requestSignature(String keyId, PreparedTx preparedTx) throws GeneralSecurityException {
        return kmsDriver.sign(keyId, preparedTx.payloadHash());
    }

    @Override
    public CompletableFuture<TransactionReceipt> submitTransaction(PreparedTx preparedTx, byte[] signature) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Function execute = new Function(
                        "execute",
                        Arrays.asList(
                                new Address(preparedTx.target()),
                                new Uint256(preparedTx.value()),
                                new DynamicBytes(preparedTx.callData()),
                                new DynamicBytes(signature)
                        ),
                        Collections.emptyList()
                );
                String callData = FunctionEncoder.encode(execute);

                BigInteger nonce = web3j.ethGetTransactionCount(
                        relayerCredentials.getAddress(), DefaultBlockParameterName.LATEST).send().getTransactionCount();

                RawTransaction tx = RawTransaction.createTransaction(
                        nonce, gasPrice(), gasLimit(), preparedTx.accountAddress(), callData);

                byte[] signed = TransactionEncoder.signMessage(tx, chainId, relayerCredentials);
                String txHash = web3j.ethSendRawTransaction(Numeric.toHexString(signed)).send().getTransactionHash();

                return pollForReceipt(txHash);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private BigInteger readAccountNonce(String accountAddress) throws Exception {
        Function nonceFn = new Function("nonce", Collections.emptyList(), Collections.emptyList());
        String encoded = FunctionEncoder.encode(nonceFn);
        var response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        relayerCredentials.getAddress(), accountAddress, encoded),
                DefaultBlockParameterName.LATEST).send();
        return Numeric.decodeQuantity(response.getValue());
    }

    private TransactionReceipt pollForReceipt(String txHash) throws Exception {
        // PoC-grade polling. Replace with a proper subscription/backoff for anything real.
        for (int i = 0; i < 40; i++) {
            var receiptOpt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            if (receiptOpt.isPresent()) return receiptOpt.get();
            Thread.sleep(500);
        }
        throw new IllegalStateException("Timed out waiting for receipt: " + txHash);
    }

    private static byte[] encodePadded(org.web3j.abi.datatypes.Type<?> type) {
        return Numeric.hexStringToByteArray(TypeEncoder.encode(type));
    }

    private static byte[] concat(byte[]... arrays) {
        int len = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] a : arrays) { System.arraycopy(a, 0, out, pos, a.length); pos += a.length; }
        return out;
    }

    private static BigInteger gasPrice() { return BigInteger.valueOf(0); } // typical for permissioned/free-gas Besu networks
    private static BigInteger gasLimit() { return BigInteger.valueOf(3_000_000); }

    // Placeholder: compile solidity/PqcAccount.sol (e.g. via solc or web3j's solidity gradle plugin)
    // and paste the resulting bytecode hex here, or load it from a build artifact at runtime.
    private static final String PQC_ACCOUNT_BYTECODE = "0x"; // TODO: fill in from compiled artifact
}
