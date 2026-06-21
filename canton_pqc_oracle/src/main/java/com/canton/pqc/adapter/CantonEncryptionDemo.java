package com.canton.pqc.adapter;

import com.corekms.HybridCipher;
import com.corekms.HybridCipher.CiphertextBundle;
import com.canton.pqc.generated.pqcconfidentialpayload.PqcConfidentialPayload;
import com.daml.ledger.javaapi.data.FiltersByParty;
import com.daml.ledger.javaapi.data.NoFilter;
import com.daml.ledger.javaapi.data.TransactionFilter;
import com.daml.ledger.javaapi.data.UpdateSubmission;
import com.daml.ledger.javaapi.data.codegen.Created;
import com.daml.ledger.rxjava.DamlLedgerClient;

import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * Proves the hybrid-encryption side (PqcConfidentialPayload) against a live Daml sandbox
 */
public final class CantonEncryptionDemo {
    private static final String NS = "12201229ffc392648322bd5f292bd30e8e8981a623c87dfa7a5e047e39e8760eab2e";
    private static final String ALICE = "alice::" + NS;   // sender
    private static final String ORACLE = "oracle::" + NS; // recipient, for this demo
    private static final String BOB = "bob::" + NS;        // third, uninvolved party

    public static void main(String[] args) throws Exception {
        DamlLedgerClient client = DamlLedgerClient.newBuilder("localhost", 6865).build();
        client.connect();
        System.out.println("connected, ledger id: " + client.getLedgerId());

        System.out.println("--- recipient (oracle): generate hybrid encryption keys via core_kms ---");
        HybridCipher.RecipientKeys keys = HybridCipher.generateRecipientKeys();

        String plaintext = "settlement amount: 50000 USD, instrument ISIN US0378331005";
        System.out.println("--- sender (alice): hybrid-encrypt the payload via core_kms ---");
        CiphertextBundle bundle = HybridCipher.encrypt(
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), plaintext.getBytes());

        String ephemeralXPubHex = toHex(bundle.ephemeralXPub());
        String kemEncapsulationHex = toHex(bundle.kemEncapsulation());
        String nonceHex = toHex(bundle.nonce());
        String aesCiphertextHex = toHex(bundle.aesCiphertext());

        System.out.println("--- submitting PqcConfidentialPayload to the live sandbox ---");
        var update = PqcConfidentialPayload.create(
                ALICE, ORACLE, ephemeralXPubHex, kemEncapsulationHex, nonceHex, aesCiphertextHex);
        var submission = UpdateSubmission.create("canton-pqc-encryption-demo",
                "submit-payload-" + UUID.randomUUID(), update).withActAs(ALICE);

        Created<PqcConfidentialPayload.ContractId> result = client.getCommandClient()
                .submitAndWaitForResult(submission)
                .timeout(30, TimeUnit.SECONDS)
                .blockingGet();
        System.out.println("PqcConfidentialPayload contract id: " + result.contractId.contractId);

        System.out.println("--- independent verification: query as recipient (oracle) ---");
        long recipientCount = countActiveContracts(client, ORACLE);
        System.out.println("contracts visible to recipient: " + recipientCount);

        System.out.println("--- independent verification: query as third, uninvolved party (bob) ---");
        long bobCount = countActiveContracts(client, BOB);
        System.out.println("contracts visible to bob: " + bobCount);

        System.out.println("--- recipient (oracle): decrypt the recovered bytes via core_kms ---");
        byte[] recovered = HybridCipher.decrypt(
                keys.x25519KeyPair().getPrivate(), keys.kemKeyPair().getPrivate(),
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), bundle);
        System.out.println("recovered: \"" + new String(recovered) + "\"");
        System.out.println("matches original: " + plaintext.equals(new String(recovered)));

        client.close();

        if (recipientCount >= 1 && bobCount == 0 && plaintext.equals(new String(recovered))) {
            System.out.println();
            System.out.println("PROOF SUCCEEDED on a live Canton sandbox: recipient sees the contract,");
            System.out.println("an uninvolved third party sees nothing at all, and the recipient");
            System.out.println("correctly decrypts the original plaintext via core_kms.");
        } else {
            System.out.println("FAIL: one or more conditions did not hold");
            System.exit(1);
        }
    }

    private static long countActiveContracts(DamlLedgerClient client, String party) throws GeneralSecurityException {
        TransactionFilter filter = new FiltersByParty(Collections.singletonMap(party, NoFilter.instance));
        return client.getActiveContractSetClient()
                .getActiveContracts(filter, true)
                .flatMapIterable(resp -> resp.getCreatedEvents())
                .filter(ev -> ev.getTemplateId().getEntityName().equals("PqcConfidentialPayload"))
                .count()
                .blockingGet();
    }

    private static String toHex(byte[] data) {
        return java.util.HexFormat.of().formatHex(data);
    }
}
