# core-kms

The chain-agnostic layer.

## Package: `com.corekms`

Core key generation, signing, verification, and hybrid encryption functions 

- `HybridCipher.java` — X25519 + ML-KEM-768 hybrid encryption.
- `kms/KmsDriver.java` — the abstraction layer
- `kms/SoftwareKmsDriver.java` — BouncyCastle backend, dev/test only.
- `kms/EntrustHsmKmsDriver.java` — Entrust nShield backend, via PKCS#11
- `kms/KmsDriverFactory.java` — picks the backend by config

## Build

`mvn install` here first — consuming projects resolve this from the local
Maven repository via the `com.corekms:core-kms:1.0.0` coordinates.

## Adding a new chain
A new chain adapter should depend on this artifact and implement only:
1. How it represents an "intent" to authorize (whatever that chain's native
   transaction/command shape is).
2. How it turns `KmsDriver.verify`'s boolean result into something that
   chain's nodes actually enforce — a smart-contract/precompile check
   (consensus-enforced, for programmable-execution chains) or an oracle
   attestation (for fixed-scheme ledgers). Nothing in this module needs to
   change to add that chain.
