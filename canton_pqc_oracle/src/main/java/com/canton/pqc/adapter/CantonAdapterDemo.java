package com.canton.pqc.adapter;

import com.corekms.kms.SoftwareKmsDriver;
import com.daml.ledger.javaapi.data.FiltersByParty;
import com.daml.ledger.javaapi.data.NoFilter;
import com.daml.ledger.javaapi.data.TransactionFilter;
import com.daml.ledger.rxjava.DamlLedgerClient;

import java.util.Collections;

/** Throwaway runner to exercise CantonAdapter against a real local sandbox. */
public final class CantonAdapterDemo {
    private static final String ALICE =
            "alice::12206c5054e89386dfa16a7e80f416c91e9f8a6df187c476f558c9068b8402efc084";
    private static final String ORACLE =
            "oracle::12206c5054e89386dfa16a7e80f416c91e9f8a6df187c476f558c9068b8402efc084";

    public static void main(String[] args) throws Exception {
        DamlLedgerClient client = DamlLedgerClient.newBuilder("localhost", 6865).build();
        client.connect();
        System.out.println("connected, ledger id: " + client.getLedgerId());

        CantonAdapter adapter = new CantonAdapter(client, new SoftwareKmsDriver(), "canton-pqc-demo");

        System.out.println("--- registerIdentity(alice) [skipping if it already exists from a prior run] ---");
        try {
            String identityContractId = adapter.registerIdentity(ALICE);
            System.out.println("PqcIdentity contract id: " + identityContractId);
        } catch (Exception e) {
            System.out.println("already exists (expected on a repeat run): " + e.getMessage());
        }

        System.out.println("--- attestation flow: alice -> oracle ---");
        byte[] payload = "transfer 100 units to bob".getBytes();
        var intent = new CantonAdapter.AttestationIntent(ALICE, ORACLE, payload);
        var prepared = adapter.prepareTransaction(intent);
        byte[] signature = adapter.requestSignature(ALICE, prepared);
        String requestContractId = adapter.submitTransaction(prepared, signature);
        System.out.println("PqcAttestationRequest contract id: " + requestContractId);

        System.out.println("--- independent verification: query the active contract set as alice ---");
        TransactionFilter filter = new FiltersByParty(
                Collections.singletonMap(ALICE, NoFilter.instance));
        client.getActiveContractSetClient()
                .getActiveContracts(filter, true)
                .doOnNext(resp -> resp.getCreatedEvents().forEach(ev ->
                        System.out.println("  active contract: " + ev.getTemplateId() + " / " + ev.getContractId())))
                .blockingSubscribe();

        client.close();
    }
}
