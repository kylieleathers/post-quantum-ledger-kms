package com.corekms;

import com.corekms.HybridCipher.CiphertextBundle;
import com.corekms.HybridCipher.RecipientKeys;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class HybridCipherTest {

    @Test
    void encryptThenDecrypt_recoversOriginalPlaintext() throws GeneralSecurityException {
        RecipientKeys keys = HybridCipher.generateRecipientKeys();
        byte[] plaintext = "settlement instruction payload".getBytes();

        CiphertextBundle bundle = HybridCipher.encrypt(
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), plaintext);
        byte[] recovered = HybridCipher.decrypt(
                keys.x25519KeyPair().getPrivate(), keys.kemKeyPair().getPrivate(),
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(),
                bundle);

        assertArrayEquals(plaintext, recovered);
    }

    @Test
    void encrypt_isNonDeterministic_dueToEphemeralKeyAndNonce() throws GeneralSecurityException {
        RecipientKeys keys = HybridCipher.generateRecipientKeys();
        byte[] plaintext = "same plaintext".getBytes();

        CiphertextBundle first = HybridCipher.encrypt(
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), plaintext);
        CiphertextBundle second = HybridCipher.encrypt(
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), plaintext);

        assertFalse(Arrays.equals(first.aesCiphertext(), second.aesCiphertext()),
                "two encryptions of the same plaintext should not produce identical ciphertext");
    }

    @Test
    void decrypt_withWrongRecipientKeys_fails() throws GeneralSecurityException {
        RecipientKeys recipient = HybridCipher.generateRecipientKeys();
        RecipientKeys wrongRecipient = HybridCipher.generateRecipientKeys();
        byte[] plaintext = "should not be readable by the wrong recipient".getBytes();

        CiphertextBundle bundle = HybridCipher.encrypt(
                recipient.x25519KeyPair().getPublic(), recipient.kemKeyPair().getPublic(), plaintext);

        assertThrows(GeneralSecurityException.class, () -> HybridCipher.decrypt(
                wrongRecipient.x25519KeyPair().getPrivate(), wrongRecipient.kemKeyPair().getPrivate(),
                wrongRecipient.x25519KeyPair().getPublic(), wrongRecipient.kemKeyPair().getPublic(),
                bundle));
    }

    @Test
    void decrypt_withTamperedCiphertext_failsAuthentication() throws GeneralSecurityException {
        RecipientKeys keys = HybridCipher.generateRecipientKeys();
        byte[] plaintext = "integrity should be protected by the GCM tag".getBytes();

        CiphertextBundle bundle = HybridCipher.encrypt(
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), plaintext);

        byte[] tampered = bundle.aesCiphertext().clone();
        tampered[0] ^= 0x01; // flip a bit
        CiphertextBundle tamperedBundle = new CiphertextBundle(
                bundle.ephemeralXPub(), bundle.kemEncapsulation(), bundle.nonce(), tampered);

        assertThrows(GeneralSecurityException.class, () -> HybridCipher.decrypt(
                keys.x25519KeyPair().getPrivate(), keys.kemKeyPair().getPrivate(),
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(),
                tamperedBundle));
    }

    @Test
    void decrypt_withTamperedKemCiphertext_failsOrProducesWrongKey() throws GeneralSecurityException {
        RecipientKeys keys = HybridCipher.generateRecipientKeys();
        byte[] plaintext = "kem ciphertext is part of the authenticated binding".getBytes();

        CiphertextBundle bundle = HybridCipher.encrypt(
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), plaintext);

        byte[] tamperedKemCt = bundle.kemEncapsulation().clone();
        tamperedKemCt[0] ^= 0x01;
        CiphertextBundle tamperedBundle = new CiphertextBundle(
                bundle.ephemeralXPub(), tamperedKemCt, bundle.nonce(), bundle.aesCiphertext());

        // Either the KEM decapsulation itself fails, or it succeeds with a
        // different shared secret and the GCM tag check fails downstream —
        // either way, the original plaintext must not be recoverable.
        assertThrows(GeneralSecurityException.class, () -> HybridCipher.decrypt(
                keys.x25519KeyPair().getPrivate(), keys.kemKeyPair().getPrivate(),
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(),
                tamperedBundle));
    }
}
