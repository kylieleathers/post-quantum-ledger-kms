# canton-pqc-oracle

Hybrid PQC for Canton using off-ledger oracle-attestation on the application layer with Entrust-backed.
Canton's protocol still can't verify ML-DSA/ML-KEM natively, so PQC verification happens off-ledger here and gets recorded on-ledger via a classically-signed attestation.

**Run `mvn install` in `../core_kms` before
building this project.**

## Components

- `../core_kms/` — the shared layer: `HybridCipher`, `KmsDriver`,
- `oracle/PqcOracleService.java` — Canton-specific: watches for requests,
  calls `KmsDriver.verify` (from core-kms), exercises `Accept`/`Reject`.
- `daml/PqcIdentity.daml` — three templates:
  - `PqcIdentity` — registers a party's ML-DSA public key as ordinary ledger data.
  - `PqcAttestationRequest` — carries a payload hash + ML-DSA signature awaiting oracle review.
  - `PqcAttestation` — the oracle's verified-and-signed record; downstream
    workflows should gate on this contract's existence, not on the raw
    signature in `PqcAttestationRequest`.
- `oracle/PqcOracleService.java` — watches for requests, calls
  `KmsDriver.verify`, exercises `Accept`/`Reject`.
