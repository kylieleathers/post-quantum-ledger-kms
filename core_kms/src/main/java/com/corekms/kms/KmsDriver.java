package com.corekms.kms;

import com.corekms.HybridCipher.CiphertextBundle;

import java.security.GeneralSecurityException;

/**
 * Backends: SoftwareKmsDriver (BouncyCastle) for dev/test, EntrustHsmKmsDriver
 * for production. Callers depend only on this interface.
 */
public interface KmsDriver {

    byte[] generateSigningKey(String keyId) throws GeneralSecurityException;

    void generateEncryptionKey(String keyId) throws GeneralSecurityException;

    byte[] sign(String keyId, byte[] payload) throws GeneralSecurityException;

    boolean verify(String keyId, byte[] payload, byte[] signature) throws GeneralSecurityException;

    byte[] getSigningPublicKey(String keyId) throws GeneralSecurityException;

    CiphertextBundle encrypt(String recipientKeyId, byte[] plaintext) throws GeneralSecurityException;

    byte[] decrypt(String keyId, CiphertextBundle bundle) throws GeneralSecurityException;
}
