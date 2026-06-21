package com.quantum.pqc.kms;

import com.quantum.pqc.HybridCipher;
import com.quantum.pqc.HybridCipher.CiphertextBundle;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory KmsDriver backed by BouncyCastle. Stand-in for a real KMS/HSM
 * during PoC and local testing — see IbmHsmKmsDriver for the production path.
 *
 * Not for production use: no persistence, no access control, no audit log.
 */
public final class SoftwareKmsDriver implements KmsDriver {

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastlePQCProvider());
    }

    private static final String SIGN_ALG = "ML-DSA-65";

    private record KeyMaterial(
            KeyPair signingKeyPair,                 // ML-DSA
            HybridCipher.RecipientKeys encryptionKeys // X25519 + ML-KEM-768
    ) {}

    private final Map<String, KeyMaterial> store = new ConcurrentHashMap<>();

    @Override
    public byte[] generateSigningKey(String keyId) throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(SIGN_ALG, "BCPQC");
        KeyPair pair = gen.generateKeyPair();

        store.merge(keyId,
                new KeyMaterial(pair, null),
                (existing, ignored) -> new KeyMaterial(pair, existing.encryptionKeys()));

        return pair.getPublic().getEncoded();
    }

    @Override
    public void generateEncryptionKey(String keyId) throws GeneralSecurityException {
        HybridCipher.RecipientKeys keys = HybridCipher.generateRecipientKeys();

        store.merge(keyId,
                new KeyMaterial(null, keys),
                (existing, ignored) -> new KeyMaterial(existing.signingKeyPair(), keys));
    }

    @Override
    public byte[] sign(String keyId, byte[] payload) throws GeneralSecurityException {
        KeyMaterial km = require(keyId);
        Signature sig = Signature.getInstance(SIGN_ALG, "BCPQC");
        sig.initSign(km.signingKeyPair().getPrivate());
        sig.update(payload);
        return sig.sign();
    }

    @Override
    public boolean verify(String keyId, byte[] payload, byte[] signature) throws GeneralSecurityException {
        KeyMaterial km = require(keyId);
        Signature sig = Signature.getInstance(SIGN_ALG, "BCPQC");
        sig.initVerify(km.signingKeyPair().getPublic());
        sig.update(payload);
        return sig.verify(signature);
    }

    @Override
    public byte[] getSigningPublicKey(String keyId) throws GeneralSecurityException {
        return require(keyId).signingKeyPair().getPublic().getEncoded();
    }

    @Override
    public CiphertextBundle encrypt(String recipientKeyId, byte[] plaintext) throws GeneralSecurityException {
        HybridCipher.RecipientKeys keys = require(recipientKeyId).encryptionKeys();
        return HybridCipher.encrypt(
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), plaintext);
    }

    @Override
    public byte[] decrypt(String keyId, CiphertextBundle bundle) throws GeneralSecurityException {
        HybridCipher.RecipientKeys keys = require(keyId).encryptionKeys();
        return HybridCipher.decrypt(
                keys.x25519KeyPair().getPrivate(), keys.kemKeyPair().getPrivate(),
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(),
                bundle);
    }

    private KeyMaterial require(String keyId) {
        KeyMaterial km = store.get(keyId);
        if (km == null) throw new IllegalStateException("No key material registered for keyId: " + keyId);
        return km;
    }
}
