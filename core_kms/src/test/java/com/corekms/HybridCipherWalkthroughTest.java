package com.corekms;

import com.corekms.HybridCipher.CiphertextBundle;
import com.corekms.HybridCipher.RecipientKeys;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import java.security.*;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Narrates each step of the hybrid encrypt/decrypt flow to stdout (not a
 * correctness test — HybridCipherTest covers that). Secret material is
 * printed only as a length or short truncated hex prefix, never in full.
 */
class HybridCipherWalkthroughTest {

    private static final HexFormat HEX = HexFormat.of();

    @Test
    void walkthroughEncryptThenDecrypt() throws GeneralSecurityException {
        section("1. Generate the recipient's long-term keypairs");
        RecipientKeys recipient = HybridCipher.generateRecipientKeys();
        log("recipient X25519 public key", recipient.x25519KeyPair().getPublic().getEncoded());
        log("recipient ML-KEM-768 public key", recipient.kemKeyPair().getPublic().getEncoded());

        byte[] plaintext = "settlement instruction payload".getBytes();
        section("2. Plaintext to protect");
        System.out.println("    \"" + new String(plaintext) + "\" (" + plaintext.length + " bytes)");

        // --- ENCRYPT SIDE, mirroring HybridCipher.encrypt step by step ---

        section("3. Generate an ephemeral X25519 keypair (sender side, single use)");
        KeyPair ephemeral = KeyPairGenerator.getInstance("X25519", "BC").generateKeyPair();
        log("ephemeral public key", ephemeral.getPublic().getEncoded());

        section("4. Classical leg: ECDH(ephemeral private, recipient X25519 public) -> ss1");
        KeyAgreement ka = KeyAgreement.getInstance("X25519", "BC");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(recipient.x25519KeyPair().getPublic(), true);
        byte[] ss1 = ka.generateSecret();
        logSecret("ss1 (classical shared secret)", ss1);

        section("5. PQC leg: ML-KEM-768 encapsulation against recipient's KEM public key -> ss2 + ciphertext");
        KeyGenerator kemGen = KeyGenerator.getInstance("ML-KEM-768", "BC");
        kemGen.init(new KEMGenerateSpec(recipient.kemKeyPair().getPublic(), "AES"), new SecureRandom());
        SecretKeyWithEncapsulation senderSide = (SecretKeyWithEncapsulation) kemGen.generateKey();
        byte[] ss2 = senderSide.getEncoded();
        byte[] kemCiphertext = senderSide.getEncapsulation();
        logSecret("ss2 (PQC shared secret)", ss2);
        log("KEM ciphertext (sent to recipient)", kemCiphertext);

        section("6. Combine ss1 + ss2 via HKDF, binding public material into the info parameter");
        System.out.println("    (this is the downgrade-resistance step — see HybridCipher.combine)");

        section("7. Derive AES-256-GCM key from the combined secret, encrypt the plaintext");
        CiphertextBundle bundle = HybridCipher.encrypt(
                recipient.x25519KeyPair().getPublic(), recipient.kemKeyPair().getPublic(), plaintext);
        log("AES-GCM nonce", bundle.nonce());
        log("AES-GCM ciphertext (includes auth tag)", bundle.aesCiphertext());

        section("8. What actually gets transmitted to the recipient");
        System.out.println("    ephemeralXPub (" + bundle.ephemeralXPub().length + " bytes)");
        System.out.println("    kemEncapsulation (" + bundle.kemEncapsulation().length + " bytes)");
        System.out.println("    nonce (" + bundle.nonce().length + " bytes)");
        System.out.println("    aesCiphertext (" + bundle.aesCiphertext().length + " bytes)");
        System.out.println("    [the recipient's PRIVATE keys never appear in any of this]");

        // --- DECRYPT SIDE ---

        section("9. Recipient: recompute ss1 via ECDH using their own private key + the sender's ephemeral public key");
        section("10. Recipient: recompute ss2 via ML-KEM decapsulation using their own private key + the received ciphertext");
        section("11. Recipient: same HKDF combine -> same AES key -> AES-GCM decrypt");
        byte[] recovered = HybridCipher.decrypt(
                recipient.x25519KeyPair().getPrivate(), recipient.kemKeyPair().getPrivate(),
                recipient.x25519KeyPair().getPublic(), recipient.kemKeyPair().getPublic(),
                bundle);

        section("12. Result");
        System.out.println("    recovered: \"" + new String(recovered) + "\"");
        assertArrayEquals(plaintext, recovered);
        System.out.println("    matches original plaintext: true");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void log(String label, byte[] data) {
        String hex = HEX.formatHex(data);
        String preview = hex.length() > 32 ? hex.substring(0, 32) + "..." : hex;
        System.out.println("    " + label + ": " + data.length + " bytes, hex=" + preview);
    }

    /** Same as log(), but extra explicit that this is secret material — truncated harder, on purpose. */
    private static void logSecret(String label, byte[] data) {
        String hex = HEX.formatHex(data);
        String preview = hex.length() > 8 ? hex.substring(0, 8) + "...[redacted]" : hex;
        System.out.println("    " + label + ": " + data.length + " bytes, hex=" + preview);
    }
}
