package com.canton.pqc.oracle;

import com.corekms.kms.KmsDriver;

import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches for PqcAttestationRequest contracts (PqcIdentity.daml),
 * independently re-verifies the ML-DSA signature via KmsDriver (Entrust in
 * production, software in tests), and exercises Accept/Reject signed with
 * the oracle's own classical Canton key.
 */
public final class PqcOracleService {

    private static final Logger log = Logger.getLogger(PqcOracleService.class.getName());

    private final KmsDriver kmsDriver;
    private final String oracleParty;

    public PqcOracleService(KmsDriver kmsDriver, String oracleParty) {
        this.kmsDriver = kmsDriver;
        this.oracleParty = oracleParty;
    }

    public void run() throws InterruptedException {
        log.info("Starting PQC oracle for party " + oracleParty);
        // TODO: replace with a real Ledger API subscription filtered to
        // PqcAttestationRequest contracts where `oracle` == oracleParty.
        while (true) {
            for (AttestationRequestView req : pollPendingAttestationRequests()) {
                try {
                    handle(req);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed handling attestation request " + req.contractId(), e);
                }
            }
            TimeUnit.SECONDS.sleep(2);
        }
    }

    private void handle(AttestationRequestView req) throws GeneralSecurityException {
        byte[] payloadHash = Base64.getDecoder().decode(req.payloadHashB64());
        byte[] signature = Base64.getDecoder().decode(req.signatureB64());

        boolean valid = kmsDriver.verify(req.party(), payloadHash, signature);

        if (valid) {
            log.info("ML-DSA signature valid for party " + req.party() + " — exercising Accept");
            submitChoice(req.contractId(), "Accept");
        } else {
            log.warning("ML-DSA signature INVALID for party " + req.party() + " — exercising Reject");
            submitChoice(req.contractId(), "Reject");
        }
    }

    // --- Ledger API integration points (sketch only) ---

    private java.util.List<AttestationRequestView> pollPendingAttestationRequests() {
        // TODO: query the active contract set for PqcAttestationRequest
        // contracts observed by oracleParty, and track already-handled
        // contract IDs to avoid double-submitting Accept/Reject.
        return java.util.List.of();
    }

    private void submitChoice(String contractId, String choiceName) {
        // TODO: build and submit ExerciseCommand(contractId, choiceName)
        // signed by oracleParty via the Ledger API.
        log.info("[stub] would submit " + choiceName + " on " + contractId + " as " + oracleParty);
    }

    /** Flattened view of a PqcAttestationRequest contract's fields, for this service's own use. */
    public record AttestationRequestView(
            String contractId,
            String party,
            String oracle,
            String payloadHashB64,
            String signatureB64
    ) {}
}
