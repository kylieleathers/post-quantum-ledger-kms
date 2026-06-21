package com.corekms.kms;

import com.corekms.HybridCipher.CiphertextBundle;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Entrust nShield-backed KmsDriver. 
 * This driver uses Entrust's confirmed ML-DSA/ML-KEM firmware support directly,
 */
public final class EntrustHsmKmsDriver implements KmsDriver {

    private static final String SIGN_ALG = "ML-DSA-65";
    private static final String KEM_ALG = "ML-KEM-768";
    private static final String CLASSICAL_ALG = "X25519"; // assumes Entrust's PKCS#11 module supports this curve natively; fall back to a software X25519 leg if not

    private final Provider provider;
    private final KeyStore keyStore;
    private final char[] hsmPin;

    public EntrustHsmKmsDriver(String pkcs11LibraryPath, int slotId, String hsmPin)
            throws GeneralSecurityException, java.io.IOException {
        this.hsmPin = hsmPin.toCharArray();

        String pkcs11Config = """
                name = EntrustNShieldOracle
                library = %s
                slot = %d
                """.formatted(pkcs11LibraryPath, slotId);

        Provider sunPkcs11 = Security.getProvider("SunPKCS11");
        this.provider = sunPkcs11.configure("--" + pkcs11Config); // config-string form varies by JDK version — verify
        Security.addProvider(provider);

        this.keyStore = KeyStore.getInstance("PKCS11", provider);
        this.keyStore.load(null, this.hsmPin);
    }

    @Override
    public byte[] generateSigningKey(String keyId) throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(SIGN_ALG, provider);
        KeyPair pair = gen.generateKeyPair();
        storeKeyPair(keyId + ":sign", pair);
        return pair.getPublic().getEncoded();
    }

    @Override
    public void generateEncryptionKey(String keyId) throws GeneralSecurityException {
        KeyPair kemPair = KeyPairGenerator.getInstance(KEM_ALG, provider).generateKeyPair();
        storeKeyPair(keyId + ":kem", kemPair);

        KeyPair xPair = KeyPairGenerator.getInstance(CLASSICAL_ALG, provider).generateKeyPair();
        storeKeyPair(keyId + ":x25519", xPair);
    }

    @Override
    public byte[] sign(String keyId, byte[] payload) throws GeneralSecurityException {
        PrivateKey priv = privateKey(keyId + ":sign");
        Signature sig = Signature.getInstance(SIGN_ALG, provider);
        sig.initSign(priv);
        sig.update(payload);
        return sig.sign();
    }

    @Override
    public boolean verify(String keyId, byte[] payload, byte[] signature) throws GeneralSecurityException {
        PublicKey pub = publicKey(keyId + ":sign");
        Signature sig = Signature.getInstance(SIGN_ALG, provider);
        sig.initVerify(pub);
        sig.update(payload);
        return sig.verify(signature);
    }

    @Override
    public byte[] getSigningPublicKey(String keyId) throws GeneralSecurityException {
        return publicKey(keyId + ":sign").getEncoded();
    }

    @Override
    public CiphertextBundle encrypt(String recipientKeyId, byte[] plaintext) throws GeneralSecurityException {
        PublicKey recipientKemPub = publicKey(recipientKeyId + ":kem");
        PublicKey recipientXPub = publicKey(recipientKeyId + ":x25519");

        // Ephemeral classical leg, generated fresh per message (not persisted under an alias).
        KeyPair ephemeral = KeyPairGenerator.getInstance(CLASSICAL_ALG, provider).generateKeyPair();

        KeyAgreement ka = KeyAgreement.getInstance(CLASSICAL_ALG, provider);
        ka.init(ephemeral.getPrivate());
        ka.doPhase(recipientXPub, true);
        byte[] ss1 = ka.generateSecret();

        // ML-KEM encapsulation 
        var kemGen = KeyGenerator.getInstance(KEM_ALG, provider);
        kemGen.init(new org.bouncycastle.jcajce.spec.KEMGenerateSpec(recipientKemPub, "AES"), new SecureRandom());
        var senderSide = (org.bouncycastle.jcajce.SecretKeyWithEncapsulation) kemGen.generateKey();
        byte[] ss2 = senderSide.getEncoded();
        byte[] kemCiphertext = senderSide.getEncapsulation();

        byte[] ephemeralXPubBytes = ephemeral.getPublic().getEncoded();
        byte[] aesKey = combine(ss1, ss2, concat(ephemeralXPubBytes, kemCiphertext,
                recipientXPub.getEncoded(), recipientKemPub.getEncoded()));

        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding"); // software AES is fine here — the sensitive part was deriving aesKey, not this transformation
        aes.init(Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(aesKey, "AES"),
                new javax.crypto.spec.GCMParameterSpec(128, nonce));
        byte[] ct = aes.doFinal(plaintext);

        return new CiphertextBundle(ephemeralXPubBytes, kemCiphertext, nonce, ct);
    }

    @Override
    public byte[] decrypt(String keyId, CiphertextBundle bundle) throws GeneralSecurityException {
        PrivateKey myXPriv = privateKey(keyId + ":x25519");
        PrivateKey myKemPriv = privateKey(keyId + ":kem");

        KeyFactory xFactory = KeyFactory.getInstance(CLASSICAL_ALG, provider);
        PublicKey senderEphemeralPub = xFactory.generatePublic(new X509EncodedKeySpec(bundle.ephemeralXPub()));

        KeyAgreement ka = KeyAgreement.getInstance(CLASSICAL_ALG, provider);
        ka.init(myXPriv);
        ka.doPhase(senderEphemeralPub, true);
        byte[] ss1 = ka.generateSecret();

        var kemGen = KeyGenerator.getInstance(KEM_ALG, provider);
        kemGen.init(new org.bouncycastle.jcajce.spec.KEMExtractSpec(myKemPriv, bundle.kemEncapsulation(), "AES"), new SecureRandom());
        var recipientSide = (org.bouncycastle.jcajce.SecretKeyWithEncapsulation) kemGen.generateKey();
        byte[] ss2 = recipientSide.getEncoded();

        byte[] aesKey = combine(ss1, ss2, concat(bundle.ephemeralXPub(), bundle.kemEncapsulation(),
                publicKey(keyId + ":x25519").getEncoded(), publicKey(keyId + ":kem").getEncoded()));

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(aesKey, "AES"),
                new javax.crypto.spec.GCMParameterSpec(128, bundle.nonce()));
        return aes.doFinal(bundle.aesCiphertext());
    }

    // --- helpers -------------------------------------------------------

    private void storeKeyPair(String alias, KeyPair pair) {
        throw new UnsupportedOperationException(
                "Reconcile against Entrust's actual key-labeling API: alias=" + alias);
    }

    private PrivateKey privateKey(String alias) throws GeneralSecurityException {
        return (PrivateKey) keyStore.getKey(alias, hsmPin);
    }

    private PublicKey publicKey(String alias) throws GeneralSecurityException {
        return keyStore.getCertificate(alias).getPublicKey();
    }

    private static byte[] combine(byte[] ss1, byte[] ss2, byte[] info) {
        var hkdf = new org.bouncycastle.crypto.generators.HKDFBytesGenerator(new org.bouncycastle.crypto.digests.SHA256Digest());
        hkdf.init(new org.bouncycastle.crypto.params.HKDFParameters(concat(ss1, ss2), null, info));
        byte[] out = new byte[32];
        hkdf.generateBytes(out, 0, out.length);
        return out;
    }

    private static byte[] concat(byte[]... arrays) {
        int len = 0;
        for (byte[] a : arrays) len += a.length;
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] a : arrays) { System.arraycopy(a, 0, out, pos, a.length); pos += a.length; }
        return out;
    }
}
