package com.quantum.pqc.precompile;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.precompile.AbstractPrecompiledContract;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * Custom Besu precompiled contract: ML-DSA-65 signature verification, run
 * natively in Java via BouncyCastle instead of reimplemented in Solidity.
 * Every validator on the network loads this same plugin, so every node
 * verifies identically — same consensus guarantee as a built-in precompile.
 *
 * Calldata layout (all lengths big-endian uint32, then the bytes):
 *   [4: pubKeyLen][pubKey][4: msgLen][msg][4: sigLen][sig]
 * Returns 32 bytes: 0x00..01 if valid, 0x00..00 if invalid.
 *
 * Registered at a fixed address (see PqcBesuPlugin) — only usable on a
 * private/permissioned network where you control which precompiles every
 * validator loads.
 */
public class MlDsaVerifyPrecompile extends AbstractPrecompiledContract {

    private static final String SIGN_ALG = "ML-DSA-65";

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastlePQCProvider());
    }

    public MlDsaVerifyPrecompile(GasCalculator gasCalculator) {
        super("MlDsaVerify", gasCalculator);
    }

    @Override
    public long gasRequirement(Bytes input) {
        // Flat cost for the PoC. Tune based on measured verification time
        // before using this on anything beyond a local test network.
        return 50_000L;
    }

    @Override
    public Bytes computePrecompile(Bytes input, MessageFrame messageFrame) {
        try {
            int offset = 0;
            int pubKeyLen = readUint32(input, offset); offset += 4;
            byte[] pubKeyBytes = input.slice(offset, pubKeyLen).toArray(); offset += pubKeyLen;

            int msgLen = readUint32(input, offset); offset += 4;
            byte[] msgBytes = input.slice(offset, msgLen).toArray(); offset += msgLen;

            int sigLen = readUint32(input, offset); offset += 4;
            byte[] sigBytes = input.slice(offset, sigLen).toArray();

            KeyFactory kf = KeyFactory.getInstance(SIGN_ALG, "BCPQC");
            PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubKeyBytes));

            Signature verifier = Signature.getInstance(SIGN_ALG, "BCPQC");
            verifier.initVerify(pubKey);
            verifier.update(msgBytes);
            boolean valid = verifier.verify(sigBytes);

            byte[] result = new byte[32];
            if (valid) result[31] = 1;
            return Bytes.wrap(result);

        } catch (Exception e) {
            // Malformed input or verification failure both resolve to "invalid" —
            // never throw out of a precompile, that would diverge node behavior.
            return Bytes.wrap(new byte[32]);
        }
    }

    private static int readUint32(Bytes input, int offset) {
        return input.slice(offset, 4).toInt();
    }
}
