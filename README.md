# A Chain-Agnostic Post-Quantum Cryptography Layer

## Mathematical overview

This project uses a hybrid cryptographic construction that combines a classical
Diffie–Hellman shared secret (X25519) with a post-quantum key-encapsulation
mechanism (ML-KEM-768). The two shared secrets are concatenated and fed into
an HKDF to derive a symmetric key used with AES-256-GCM for confidentiality.

- Classical shared secret: $s_1 = \mathrm{ECDH}(sk_{eph}, pk_{recip})$
- PQC shared secret: $s_2 = \mathrm{Decaps}(sk_{recip}, c)$
- Symmetric key derivation: $K = \mathrm{HKDF}(s_1 \| s_2, \mathrm{info})$

The resulting key $K$ is used with an AEAD (AES-256-GCM) to produce ciphertext
and authentication tag. This hybrid approach provides defense-in-depth: even
if one primitive is weakened in the future, the combined construction retains
security provided the other primitive remains hard to break.

Signatures use the ML-DSA-65 scheme for post-quantum authentication; message
signing and verification follow the standard API shape $\mathrm{Sign}_{sk}(m)$
and $\mathrm{Verify}_{pk}(m,\sigma)$ as implemented in `core_kms`.

A brief sequence for encryption:

$$
s_1 = \mathrm{ECDH}(sk_{eph}, pk_{recip}),\qquad s_2 = \mathrm{Decaps}(sk_{recip}, c)\\
K = \mathrm{HKDF}(s_1\|s_2,\mathrm{info}),\\
\mathrm{ciphertext} = \mathrm{AES\text{-}GCM}_K(\mathrm{nonce},\mathrm{plaintext})
$$

A short rationale: combining classical ECDH with a PQC KEM gives immediate
interoperability with existing systems (ECDH) while adding quantum-resistant
hardness via the ML-KEM primitive.

This project is proven against two structurally unrelated ledgers — Canton and an EVM chain — using the identical, unmodified core implementation. 

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

