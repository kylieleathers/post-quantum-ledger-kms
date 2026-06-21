package com.corekms.kms;

import com.corekms.HybridCipher;
import com.corekms.HybridCipher.CiphertextBundle;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Dev/test backend — no HSM required. Use EntrustHsmKmsDriver for anything real. */
public final class SoftwareKmsDriver implements KmsDriver {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String SIGN_ALG = "ML-DSA-65";

    private record KeyMaterial(KeyPair signingKeyPair, HybridCipher.RecipientKeys encryptionKeys) {}

    private final Map<String, KeyMaterial> store = new ConcurrentHashMap<>();

    @Override
    public byte[] generateSigningKey(String keyId) throws GeneralSecurityException {
        KeyPair pair = KeyPairGenerator.getInstance(SIGN_ALG, "BC").generateKeyPair();
        store.merge(keyId, new KeyMaterial(pair, null),
                (existing, ignored) -> new KeyMaterial(pair, existing.encryptionKeys()));
        return pair.getPublic().getEncoded();
    }

    @Override
    public void generateEncryptionKey(String keyId) throws GeneralSecurityException {
        HybridCipher.RecipientKeys keys = HybridCipher.generateRecipientKeys();
        store.merge(keyId, new KeyMaterial(null, keys),
                (existing, ignored) -> new KeyMaterial(existing.signingKeyPair(), keys));
    }

    @Override
    public byte[] sign(String keyId, byte[] payload) throws GeneralSecurityException {
        Signature sig = Signature.getInstance(SIGN_ALG, "BC");
        sig.initSign(require(keyId).signingKeyPair().getPrivate());
        sig.update(payload);
        return sig.sign();
    }

    @Override
    public boolean verify(String keyId, byte[] payload, byte[] signature) throws GeneralSecurityException {
        Signature sig = Signature.getInstance(SIGN_ALG, "BC");
        sig.initVerify(require(keyId).signingKeyPair().getPublic());
        sig.update(payload);
        return sig.verify(signature);
    }

    @Override
    public byte[] getSigningPublicKey(String keyId) {
        return require(keyId).signingKeyPair().getPublic().getEncoded();
    }

    @Override
    public CiphertextBundle encrypt(String recipientKeyId, byte[] plaintext) throws GeneralSecurityException {
        var keys = require(recipientKeyId).encryptionKeys();
        return HybridCipher.encrypt(keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), plaintext);
    }

    @Override
    public byte[] decrypt(String keyId, CiphertextBundle bundle) throws GeneralSecurityException {
        var keys = require(keyId).encryptionKeys();
        return HybridCipher.decrypt(keys.x25519KeyPair().getPrivate(), keys.kemKeyPair().getPrivate(),
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), bundle);
    }

    private KeyMaterial require(String keyId) {
        KeyMaterial km = store.get(keyId);
        if (km == null) throw new IllegalStateException("No key material for keyId: " + keyId);
        return km;
    }
}
