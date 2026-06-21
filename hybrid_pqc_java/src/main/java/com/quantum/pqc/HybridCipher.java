package com.quantum.pqc;

// Deps: org.bouncycastle:bcprov-jdk18on:1.79+, bcpqc-jdk18on:1.79+
//
// BouncyCastle's PQC API has been renamed/restructured across releases
// (Kyber -> ML-KEM) — verify class/algorithm-name strings against the BC
// jar version on the classpath before relying on this beyond a PoC.

import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Hybrid classical + post-quantum encryption: combines X25519 (ECDH) and
 * ML-KEM-768 shared secrets via HKDF, binding both transcripts into the KDF
 * context to prevent downgrade/stripping attacks. Same shape as TLS 1.3's
 * X25519MLKEM768 group / Signal's PQXDH.
 */
public final class HybridCipher {

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastlePQCProvider());
    }

    private static final String EC_ALG = "X25519";
    private static final String KEM_ALG = "ML-KEM-768";
    private static final String AES_ALG = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_NONCE_BYTES = 12;

    /** Bundle transmitted from sender to recipient. */
    public record CiphertextBundle(
            byte[] ephemeralXPub,    // sender's ephemeral X25519 public key
            byte[] kemEncapsulation, // ML-KEM ciphertext
            byte[] nonce,
            byte[] aesCiphertext     // includes GCM tag
    ) {}

    public record RecipientKeys(
            KeyPair x25519KeyPair,
            KeyPair kemKeyPair
    ) {}

    public static RecipientKeys generateRecipientKeys() throws GeneralSecurityException {
        KeyPairGenerator xGen = KeyPairGenerator.getInstance(EC_ALG, "BC");
        KeyPair xPair = xGen.generateKeyPair();

        KeyPairGenerator kemGen = KeyPairGenerator.getInstance(KEM_ALG, "BCPQC");
        KeyPair kemPair = kemGen.generateKeyPair();

        return new RecipientKeys(xPair, kemPair);
    }

    public static CiphertextBundle encrypt(
            PublicKey recipientXPub,
            PublicKey recipientKemPub,
            byte[] plaintext
    ) throws GeneralSecurityException {

        // 1. Ephemeral X25519 keypair + classical ECDH shared secret
        KeyPairGenerator xGen = KeyPairGenerator.getInstance(EC_ALG, "BC");
        KeyPair ephemeral = xGen.generateKeyPair();

        KeyAgreement ka = KeyAgreement.getInstance(EC_ALG, "BC");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(recipientXPub, true);
        byte[] ss1 = ka.generateSecret();

        // 2. ML-KEM encapsulation against recipient's KEM public key
        KeyGenerator kemGen = KeyGenerator.getInstance(KEM_ALG, "BCPQC");
        kemGen.init(new KEMGenerateSpec(recipientKemPub, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation senderSide = (SecretKeyWithEncapsulation) kemGen.generateKey();
        byte[] ss2 = senderSide.getEncoded();
        byte[] kemCiphertext = senderSide.getEncapsulation();

        byte[] ephemeralXPubBytes = ephemeral.getPublic().getEncoded();
        byte[] recipientXPubBytes = recipientXPub.getEncoded();
        byte[] recipientKemPubBytes = recipientKemPub.getEncoded();

        // 3. Combine: HKDF over (ss1 || ss2), binding all public material into `info`
        //    to prevent an attacker stripping the PQC component (downgrade resistance).
        byte[] aesKeyBytes = combine(ss1, ss2,
                concat(ephemeralXPubBytes, kemCiphertext, recipientXPubBytes, recipientKemPubBytes));

        // 4. AES-256-GCM encrypt the actual payload under the derived key
        byte[] nonce = new byte[GCM_NONCE_BYTES];
        new SecureRandom().nextBytes(nonce);

        Cipher aes = Cipher.getInstance(AES_ALG, "BC");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKeyBytes, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] ct = aes.doFinal(plaintext);

        return new CiphertextBundle(ephemeralXPubBytes, kemCiphertext, nonce, ct);
    }

    public static byte[] decrypt(
            PrivateKey myXPriv,
            PrivateKey myKemPriv,
            PublicKey myXPub,
            PublicKey myKemPub,
            CiphertextBundle bundle
    ) throws GeneralSecurityException {

        KeyFactory xFactory = KeyFactory.getInstance(EC_ALG, "BC");
        PublicKey senderEphemeralPub = xFactory.generatePublic(
                new java.security.spec.X509EncodedKeySpec(bundle.ephemeralXPub()));

        // 1. Recompute classical shared secret
        KeyAgreement ka = KeyAgreement.getInstance(EC_ALG, "BC");
        ka.init(myXPriv);
        ka.doPhase(senderEphemeralPub, true);
        byte[] ss1 = ka.generateSecret();

        // 2. Recompute PQC shared secret via decapsulation
        KeyGenerator kemGen = KeyGenerator.getInstance(KEM_ALG, "BCPQC");
        kemGen.init(new KEMExtractSpec(myKemPriv, bundle.kemEncapsulation(), "AES"), new SecureRandom());
        SecretKeyWithEncapsulation recipientSide = (SecretKeyWithEncapsulation) kemGen.generateKey();
        byte[] ss2 = recipientSide.getEncoded();

        // 3. Same combine step, same binding material
        byte[] aesKeyBytes = combine(ss1, ss2,
                concat(bundle.ephemeralXPub(), bundle.kemEncapsulation(),
                        myXPub.getEncoded(), myKemPub.getEncoded()));

        // 4. Decrypt
        Cipher aes = Cipher.getInstance(AES_ALG, "BC");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKeyBytes, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, bundle.nonce()));
        return aes.doFinal(bundle.aesCiphertext());
    }

    private static byte[] combine(byte[] ss1, byte[] ss2, byte[] info) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new org.bouncycastle.crypto.digests.SHA256Digest());
        hkdf.init(new HKDFParameters(concat(ss1, ss2), null, info));
        byte[] out = new byte[32]; // AES-256 key
        hkdf.generateBytes(out, 0, out.length);
        return out;
    }

    private static byte[] concat(byte[]... arrays) {
        int len = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        ByteBuffer buf = ByteBuffer.allocate(len);
        for (byte[] a : arrays) buf.put(a);
        return buf.array();
    }

    public static void main(String[] args) throws GeneralSecurityException {
        RecipientKeys keys = generateRecipientKeys();
        byte[] message = "settlement instruction payload".getBytes();

        CiphertextBundle bundle = encrypt(
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(), message);

        byte[] recovered = decrypt(
                keys.x25519KeyPair().getPrivate(), keys.kemKeyPair().getPrivate(),
                keys.x25519KeyPair().getPublic(), keys.kemKeyPair().getPublic(),
                bundle);

        System.out.println("Original:  " + new String(message));
        System.out.println("Recovered: " + new String(recovered));
        System.out.println("Match: " + Arrays.equals(message, recovered));
    }
}
