package com.canton.pqc.adapter;

import com.corekms.adapter.ChainAdapter;
import com.corekms.kms.KmsDriver;
import com.canton.pqc.generated.pqcidentity.PqcAttestationRequest;
import com.canton.pqc.generated.pqcidentity.PqcIdentity;
import com.daml.ledger.javaapi.data.UpdateSubmission;
import com.daml.ledger.javaapi.data.codegen.Created;
import com.daml.ledger.rxjava.DamlLedgerClient;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * ChainAdapter implementation for Canton, via Daml's Ledger API bindings
 * (com.daml:bindings-rxjava). registerIdentity creates a PqcIdentity
 * contract; prepareTransaction/requestSignature/submitTransaction produce a
 * PqcAttestationRequest contract carrying an ML-DSA signature from
 * core_kms's KmsDriver as ordinary contract data — Canton's protocol never
 * verifies it (see PqcIdentity.daml), only the oracle does.
 *
 * requestSignature signs the application payload, not Canton's own
 * transaction authorization (that's the participant's job and invisible
 * here) — different from how an EVM adapter signs the on-chain tx itself.
 */
public final class CantonAdapter implements
        ChainAdapter<CantonAdapter.AttestationIntent, CantonAdapter.PreparedAttestation, String> {

    public record AttestationIntent(String partyId, String oraclePartyId, byte[] payload) {}

    public record PreparedAttestation(String partyId, String oraclePartyId, byte[] payloadHash) {}

    private final DamlLedgerClient client;
    private final KmsDriver kmsDriver;
    private final String applicationId;

    public CantonAdapter(DamlLedgerClient client, KmsDriver kmsDriver, String applicationId) {
        this.client = client;
        this.kmsDriver = kmsDriver;
        this.applicationId = applicationId;
    }

    @Override
    public String registerIdentity(String keyId) throws Exception {
        byte[] publicKey = kmsDriver.generateSigningKey(keyId);
        String publicKeyB64 = Base64.getEncoder().encodeToString(publicKey);

        var update = PqcIdentity.create(keyId, publicKeyB64);
        var submission = UpdateSubmission.create(applicationId, commandId("register-identity"), update)
                .withActAs(keyId);

        Created<PqcIdentity.ContractId> result = client.getCommandClient()
                .submitAndWaitForResult(submission)
                .timeout(30, TimeUnit.SECONDS)
                .blockingGet();

        return result.contractId.contractId;
    }

    @Override
    public PreparedAttestation prepareTransaction(AttestationIntent intent) throws GeneralSecurityException {
        byte[] payloadHash = MessageDigest.getInstance("SHA-256").digest(intent.payload());
        return new PreparedAttestation(intent.partyId(), intent.oraclePartyId(), payloadHash);
    }

    @Override
    public byte[] requestSignature(String keyId, PreparedAttestation prepared) throws GeneralSecurityException {
        return kmsDriver.sign(keyId, prepared.payloadHash());
    }

    @Override
    public String submitTransaction(PreparedAttestation prepared, byte[] signature) {
        String payloadHashB64 = Base64.getEncoder().encodeToString(prepared.payloadHash());
        String signatureB64 = Base64.getEncoder().encodeToString(signature);

        var update = PqcAttestationRequest.create(
                prepared.partyId(), prepared.oraclePartyId(), payloadHashB64, signatureB64);
        var submission = UpdateSubmission.create(applicationId, commandId("submit-attestation-request"), update)
                .withActAs(prepared.partyId());

        Created<PqcAttestationRequest.ContractId> result = client.getCommandClient()
                .submitAndWaitForResult(submission)
                .timeout(30, TimeUnit.SECONDS)
                .blockingGet();

        return result.contractId.contractId;
    }

    private static String commandId(String label) {
        return label + "-" + java.util.UUID.randomUUID();
    }
}
