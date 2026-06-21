package com.quantum.pqc.kms;

import com.quantum.pqc.HybridCipher.CiphertextBundle;

import java.security.GeneralSecurityException;

/**
 * Chain adapters (Besu, Canton, future chains) depend only on this
 * interface, never a concrete backend. Swapping software keys for an IBM
 * HSM means writing a new implementation and changing which one the
 * factory/config wires up.
 *
 * keyId is an opaque logical identifier the caller assigns (a Besu account,
 * a Canton party, etc) — the driver maps it to its own key storage.
 */
public interface KmsDriver {

    /** Generates and stores an ML-DSA signing keypair under keyId. Returns the public key (raw encoding). */
    byte[] generateSigningKey(String keyId) throws GeneralSecurityException;

    /** Generates and stores an X25519 + ML-KEM-768 encryption keypair under keyId. */
    void generateEncryptionKey(String keyId) throws GeneralSecurityException;

    /** Signs payload with the ML-DSA private key stored under keyId. */
    byte[] sign(String keyId, byte[] payload) throws GeneralSecurityException;

    /** Verifies an ML-DSA signature against the public key stored under keyId. */
    boolean verify(String keyId, byte[] payload, byte[] signature) throws GeneralSecurityException;

    /** Returns the raw-encoded ML-DSA public key for keyId, for registering on-chain (e.g. in a PqcIdentity contract). */
    byte[] getSigningPublicKey(String keyId) throws GeneralSecurityException;

    /** Hybrid-encrypts plaintext to the recipient identified by recipientKeyId. */
    CiphertextBundle encrypt(String recipientKeyId, byte[] plaintext) throws GeneralSecurityException;

    /** Hybrid-decrypts a bundle using the private keys stored under keyId. */
    byte[] decrypt(String keyId, CiphertextBundle bundle) throws GeneralSecurityException;
}
