package com.anvilpqc;

import com.corekms.HybridCipher;
import com.corekms.HybridCipher.CiphertextBundle;
import com.corekms.kms.KmsDriver;
import com.corekms.kms.SoftwareKmsDriver;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;

/**
 * CLI bridge between core_kms and shell-script-driven Anvil/cast calls.
 * Subcommands:
 *
 *   generate-and-sign <keyId> <payload>
 *     -> prints publicKeyHex, payloadHashHex, signatureHex (one per line)
 *
 *   verify <keyId> <publicKeyHex> <payloadHashHex> <signatureHex>
 *     -> prints "VALID" or "INVALID"
 *
 *   generate-recipient-keys
 *     -> prints x25519PublicHex, kemPublicHex, x25519PrivateHex, kemPrivateHex
 *        (printing private key material is fine here — it's a local proof
 *        bridging separate CLI processes, not a real KmsDriver/HSM backend)
 *
 *   encrypt <x25519PublicHex> <kemPublicHex> <payload>
 *     -> prints ephemeralXPubHex, kemEncapsulationHex, nonceHex, aesCiphertextHex
 *
 *   decrypt <x25519PrivateHex> <kemPrivateHex> <x25519PublicHex> <kemPublicHex>
 *           <ephemeralXPubHex> <kemEncapsulationHex> <nonceHex> <aesCiphertextHex>
 *     -> prints the recovered plaintext
 *
 * Fresh SoftwareKmsDriver per invocation — no shared state across runs.
 */
public final class Main {

    public static void main(String[] args) throws GeneralSecurityException {
        if (args.length == 0) {
            System.err.println("usage: generate-and-sign | verify | generate-recipient-keys | encrypt | decrypt (see source for full arg lists)");
            System.exit(1);
        }

        HexFormat hex = HexFormat.of();
        KmsDriver driver = new SoftwareKmsDriver(); // also registers the "BC" provider via its static initializer

        switch (args[0]) {
            case "generate-and-sign" -> {
                String keyId = args[1];
                byte[] payload = args[2].getBytes();

                driver.generateSigningKey(keyId);
                byte[] publicKey = driver.getSigningPublicKey(keyId);
                byte[] payloadHash = sha256(payload);
                byte[] signature = driver.sign(keyId, payloadHash);

                System.out.println(hex.formatHex(publicKey));
                System.out.println(hex.formatHex(payloadHash));
                System.out.println(hex.formatHex(signature));
            }
            case "verify" -> {
                String keyId = args[1];
                byte[] publicKey = hex.parseHex(args[2]);
                byte[] payloadHash = hex.parseHex(args[3]);
                byte[] signature = hex.parseHex(args[4]);

                // Fresh process has no record of keyId, so register the public
                // key directly. A real oracle would fetch it from the chain
                // instead of trusting a CLI argument.
                boolean valid = verifyStandalone(publicKey, payloadHash, signature);
                System.out.println(valid ? "VALID" : "INVALID");
            }
            case "generate-recipient-keys" -> {
                HybridCipher.RecipientKeys keys = HybridCipher.generateRecipientKeys();
                System.out.println(hex.formatHex(keys.x25519KeyPair().getPublic().getEncoded()));
                System.out.println(hex.formatHex(keys.kemKeyPair().getPublic().getEncoded()));
                System.out.println(hex.formatHex(keys.x25519KeyPair().getPrivate().getEncoded()));
                System.out.println(hex.formatHex(keys.kemKeyPair().getPrivate().getEncoded()));
            }
            case "encrypt" -> {
                PublicKey x25519Pub = publicKey("X25519", hex.parseHex(args[1]));
                PublicKey kemPub = publicKey("ML-KEM-768", hex.parseHex(args[2]));
                byte[] plaintext = args[3].getBytes();

                CiphertextBundle bundle = HybridCipher.encrypt(x25519Pub, kemPub, plaintext);
                System.out.println(hex.formatHex(bundle.ephemeralXPub()));
                System.out.println(hex.formatHex(bundle.kemEncapsulation()));
                System.out.println(hex.formatHex(bundle.nonce()));
                System.out.println(hex.formatHex(bundle.aesCiphertext()));
            }
            case "decrypt" -> {
                PrivateKey x25519Priv = privateKey("X25519", hex.parseHex(args[1]));
                PrivateKey kemPriv = privateKey("ML-KEM-768", hex.parseHex(args[2]));
                PublicKey x25519Pub = publicKey("X25519", hex.parseHex(args[3]));
                PublicKey kemPub = publicKey("ML-KEM-768", hex.parseHex(args[4]));
                CiphertextBundle bundle = new CiphertextBundle(
                        hex.parseHex(args[5]), hex.parseHex(args[6]), hex.parseHex(args[7]), hex.parseHex(args[8]));

                byte[] plaintext = HybridCipher.decrypt(x25519Priv, kemPriv, x25519Pub, kemPub, bundle);
                System.out.println(new String(plaintext));
            }
            default -> {
                System.err.println("unknown subcommand: " + args[0]);
                System.exit(1);
            }
        }
    }

    private static PublicKey publicKey(String algorithm, byte[] encoded) throws GeneralSecurityException {
        return KeyFactory.getInstance(algorithm, "BC").generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static PrivateKey privateKey(String algorithm, byte[] encoded) throws GeneralSecurityException {
        return KeyFactory.getInstance(algorithm, "BC").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private static boolean verifyStandalone(byte[] publicKeyBytes, byte[] payloadHash, byte[] signature)
            throws GeneralSecurityException {
        var keyFactory = java.security.KeyFactory.getInstance("ML-DSA-65", "BC");
        var publicKey = keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(publicKeyBytes));
        var sig = java.security.Signature.getInstance("ML-DSA-65", "BC");
        sig.initVerify(publicKey);
        sig.update(payloadHash);
        return sig.verify(signature);
    }

    private static byte[] sha256(byte[] data) throws GeneralSecurityException {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }
}
