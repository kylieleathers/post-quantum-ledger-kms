package com.corekms;

// X25519 + ML-KEM-768 hybrid KEM, combined via HKDF with public material
// bound into the info parameter for downgrade resistance.

import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class HybridCipher {

    // ML-KEM/ML-DSA are registered directly in BouncyCastle's "BC" provider
    // (bcprov-jdk18on) as of 1.79 — no separate "BCPQC" provider needed.
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String EC_ALG = "X25519";
    private static final String KEM_ALG = "ML-KEM-768";
    private static final String AES_ALG = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_NONCE_BYTES = 12;

    public record CiphertextBundle(
            byte[] ephemeralXPub,
            byte[] kemEncapsulation,
            byte[] nonce,
            byte[] aesCiphertext
    ) {}

    public record RecipientKeys(KeyPair x25519KeyPair, KeyPair kemKeyPair) {}

    public static RecipientKeys generateRecipientKeys() throws GeneralSecurityException {
        KeyPair xPair = KeyPairGenerator.getInstance(EC_ALG, "BC").generateKeyPair();
        KeyPair kemPair = KeyPairGenerator.getInstance(KEM_ALG, "BC").generateKeyPair();
        return new RecipientKeys(xPair, kemPair);
    }

    public static CiphertextBundle encrypt(PublicKey recipientXPub, PublicKey recipientKemPub, byte[] plaintext)
            throws GeneralSecurityException {

        KeyPair ephemeral = KeyPairGenerator.getInstance(EC_ALG, "BC").generateKeyPair();

        KeyAgreement ka = KeyAgreement.getInstance(EC_ALG, "BC");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(recipientXPub, true);
        byte[] ss1 = ka.generateSecret();

        KeyGenerator kemGen = KeyGenerator.getInstance(KEM_ALG, "BC");
        kemGen.init(new KEMGenerateSpec(recipientKemPub, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation senderSide = (SecretKeyWithEncapsulation) kemGen.generateKey();
        byte[] ss2 = senderSide.getEncoded();
        byte[] kemCiphertext = senderSide.getEncapsulation();

        byte[] ephemeralXPubBytes = ephemeral.getPublic().getEncoded();
        byte[] aesKeyBytes = combine(ss1, ss2,
                concat(ephemeralXPubBytes, kemCiphertext, recipientXPub.getEncoded(), recipientKemPub.getEncoded()));

        byte[] nonce = new byte[GCM_NONCE_BYTES];
        new SecureRandom().nextBytes(nonce);

        Cipher aes = Cipher.getInstance(AES_ALG, "BC");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKeyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] ct = aes.doFinal(plaintext);

        return new CiphertextBundle(ephemeralXPubBytes, kemCiphertext, nonce, ct);
    }

    public static byte[] decrypt(PrivateKey myXPriv, PrivateKey myKemPriv, PublicKey myXPub, PublicKey myKemPub,
                                  CiphertextBundle bundle) throws GeneralSecurityException {

        KeyFactory xFactory = KeyFactory.getInstance(EC_ALG, "BC");
        PublicKey senderEphemeralPub = xFactory.generatePublic(
                new java.security.spec.X509EncodedKeySpec(bundle.ephemeralXPub()));

        KeyAgreement ka = KeyAgreement.getInstance(EC_ALG, "BC");
        ka.init(myXPriv);
        ka.doPhase(senderEphemeralPub, true);
        byte[] ss1 = ka.generateSecret();

        KeyGenerator kemGen = KeyGenerator.getInstance(KEM_ALG, "BC");
        kemGen.init(new KEMExtractSpec(myKemPriv, bundle.kemEncapsulation(), "AES"), new SecureRandom());
        SecretKeyWithEncapsulation recipientSide = (SecretKeyWithEncapsulation) kemGen.generateKey();
        byte[] ss2 = recipientSide.getEncoded();

        byte[] aesKeyBytes = combine(ss1, ss2,
                concat(bundle.ephemeralXPub(), bundle.kemEncapsulation(), myXPub.getEncoded(), myKemPub.getEncoded()));

        Cipher aes = Cipher.getInstance(AES_ALG, "BC");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKeyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, bundle.nonce()));
        return aes.doFinal(bundle.aesCiphertext());
    }

    private static byte[] combine(byte[] ss1, byte[] ss2, byte[] info) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new org.bouncycastle.crypto.digests.SHA256Digest());
        hkdf.init(new HKDFParameters(concat(ss1, ss2), null, info));
        byte[] out = new byte[32];
        hkdf.generateBytes(out, 0, out.length);
        return out;
    }

    private static byte[] concat(byte[]... arrays) {
        int len = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        ByteBuffer buf = ByteBuffer.allocate(len);
        for (byte[] a : arrays) buf.put(a);
        return buf.array();
    }
}
