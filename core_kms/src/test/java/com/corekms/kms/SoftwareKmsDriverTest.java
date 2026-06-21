package com.corekms.kms;

import com.corekms.HybridCipher.CiphertextBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SoftwareKmsDriverTest {

    private KmsDriver driver;

    @BeforeEach
    void setUp() {
        driver = new SoftwareKmsDriver();
    }

    @Test
    void sign_thenVerify_succeedsForGenuineSignature() throws GeneralSecurityException {
        driver.generateSigningKey("alice");
        byte[] payload = "transfer 100 units to bob".getBytes();

        byte[] signature = driver.sign("alice", payload);

        assertTrue(driver.verify("alice", payload, signature));
    }

    @Test
    void verify_failsIfPayloadWasAltered() throws GeneralSecurityException {
        driver.generateSigningKey("alice");
        byte[] payload = "transfer 100 units to bob".getBytes();
        byte[] signature = driver.sign("alice", payload);

        byte[] alteredPayload = "transfer 100000 units to bob".getBytes();

        assertFalse(driver.verify("alice", alteredPayload, signature));
    }

    @Test
    void verify_failsForSignatureFromADifferentKey() throws GeneralSecurityException {
        driver.generateSigningKey("alice");
        driver.generateSigningKey("mallory");
        byte[] payload = "transfer 100 units to bob".getBytes();

        byte[] mallorysSignature = driver.sign("mallory", payload);

        assertFalse(driver.verify("alice", payload, mallorysSignature));
    }

    @Test
    void getSigningPublicKey_matchesGeneratedKey() throws GeneralSecurityException {
        byte[] generated = driver.generateSigningKey("alice");
        byte[] fetched = driver.getSigningPublicKey("alice");

        assertArrayEquals(generated, fetched);
    }

    @Test
    void encrypt_thenDecrypt_recoversPlaintext() throws GeneralSecurityException {
        driver.generateEncryptionKey("alice");
        byte[] plaintext = "confidential trade details".getBytes();

        CiphertextBundle bundle = driver.encrypt("alice", plaintext);
        byte[] recovered = driver.decrypt("alice", bundle);

        assertArrayEquals(plaintext, recovered);
    }

    @Test
    void decrypt_withWrongKeyId_fails() throws GeneralSecurityException {
        driver.generateEncryptionKey("alice");
        driver.generateEncryptionKey("bob");
        byte[] plaintext = "only alice should be able to read this".getBytes();

        CiphertextBundle bundle = driver.encrypt("alice", plaintext);

        assertThrows(GeneralSecurityException.class, () -> driver.decrypt("bob", bundle));
    }

    @Test
    void operationsOnUnknownKeyId_throwIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> driver.sign("nobody", "x".getBytes()));
        assertThrows(IllegalStateException.class, () -> driver.getSigningPublicKey("nobody"));
    }

    @Test
    void multipleKeyIds_areIndependent() throws GeneralSecurityException {
        driver.generateSigningKey("alice");
        driver.generateSigningKey("bob");
        byte[] alicePub = driver.getSigningPublicKey("alice");
        byte[] bobPub = driver.getSigningPublicKey("bob");

        assertFalse(Arrays.equals(alicePub, bobPub), "different key IDs must not collide on the same key material");
    }
}
