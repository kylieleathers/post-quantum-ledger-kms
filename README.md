# Chain-agnostic post-quantum cryptography layer.

A chain-agnostic post-quantum secure cryptography layer (`core_kms`), proven against
two structurally unrelated ledgers — Canton and an EVM chain — using the
identical, unmodified core implementation. 

## Directories

- **[`core_kms/`](core_kms/)** — The chain-agnostic cryptography core. ML-DSA-65
  signing and X25519+ML-KEM-768 hybrid encryption behind a single
  `KmsDriver` interface. Swappable backends: `SoftwareKmsDriver`
  (BouncyCastle, used today) and `EntrustHsmKmsDriver` (Entrust nShield via
  PKCS#11, built but not yet wired to real hardware).

- **[`canton_pqc_oracle/`](canton_pqc_oracle/)** — Canton/Daml integration.
  Daml templates (`PqcIdentity`, `PqcConfidentialPayload`) plus the Java
  side that calls into `core_kms` to sign identity claims and encrypt
  payloads carried on a live Canton sandbox.

- **[`canton_entrust_kms_driver/`](canton_entrust_kms_driver/)** — Canton's
  own KMS driver SPI, implemented against Entrust's KMS so Canton's
  participant node can use Entrust-backed keys directly (independent of the
  `core_kms` HSM path).

- **[`anvil_pqc_adapter/`](anvil_pqc_adapter/)** — The EVM-side proof: a
  Foundry contracts project plus a Java CLI adapter that drives an Anvil
  (local EVM) chain through `core_kms`, demonstrating the same core
  signing/encryption code works unchanged outside of Canton.

- **[`hybrid_pqc_java/`](hybrid_pqc_java/)** — PQC account abstraction for
  EVM: a Solidity `PqcAccount` contract plus the Java-side tooling to
  produce and verify the off-chain ML-DSA signatures it checks on-chain.

- **[`besu_pqc_precompile/`](besu_pqc_precompile/)** — A custom precompiled
  contract for a private Hyperledger Besu network, exposing ML-DSA
  signature verification natively to EVM bytecode instead of relying on a
  pure-Solidity implementation.

## Status

`core_kms` and the Canton integration are proven against a live sandbox.
The Anvil adapter is a working cross-chain proof harness. `besu_pqc_precompile`
is designed but not yet executed against a running Besu network.
