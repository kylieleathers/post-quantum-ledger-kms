# canton-entrust-kms-driver

Entrust nShield-backed implementation of Canton's KMS Driver interface, for
a participant's protocol-level keys (namespace, identity, signing, encryption).


Canton's protocol still only recognizes classical schemes (Ed25519/ECDSA for
signing, ECIES/RSA-OAEP for encryption) at the topology level. This driver
therefore only implements **classical** operations — what you get from
wiring this in is **hardware-custodied classical keys**, not PQC-native
Canton keys. Entrust's HSM also supports ML-DSA/ML-KEM natively, but Canton's
protocol can't verify those yet regardless of where the key lives. The PQC
side of this project lives in the separate `EntrustHsmKmsDriver` (implementing
the project's own `KmsDriver` interface from `hybrid_pqc_java/`), used for the
Besu smart-account signing and the Canton oracle-attestation pattern — not
this module.

## Deployment notes
- This must be deployed on a **new** participant, not retrofitted onto an
  existing one — the namespace key cannot be migrated in place.
  `conf/canton-entrust-kms.conf.example` assumes a fresh participant.
- Source the HSM PIN from an environment variable or secrets manager, not a
  committed conf value (the example file does this via `${ENTRUST_HSM_PIN}`).

## Next steps
1. Obtain the actual `canton-kms-driver-api` jar/source and reconcile every
   TODO above against it.
2. Confirm Entrust's exact PKCS#11 library path and slot/labeling conventions
   against your actual nShield deployment.
3. Build, deploy alongside a throwaway test participant, and verify a full
   round trip: generate a signing key, sign a topology transaction, verify it.
