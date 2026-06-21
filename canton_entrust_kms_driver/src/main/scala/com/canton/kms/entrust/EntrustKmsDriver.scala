package com.canton.kms.entrust

import com.digitalasset.canton.crypto.kms.driver.api._

import java.security._
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import scala.concurrent.{ExecutionContext, Future}

/**
 * Entrust nShield-backed implementation of Canton's KmsDriver trait.
 *
 * Drives Canton's own protocol-level keys (namespace/identity/signing/
 * encryption) via Entrust's PKCS#11 module, classical schemes only
 * (Ed25519/ECDSA, ECIES/RSA-OAEP). The PQC side lives separately in
 * core_kms's EntrustHsmKmsDriver.
 *
 * Method names/signatures are drafted from Digital Asset's documented
 * description of the required operation
 */
class EntrustKmsDriver(config: EntrustKmsDriverConfig)(implicit ec: ExecutionContext)
    extends KmsDriver {

  private val provider: Provider = {
    val pkcs11Config =
      s"""name = EntrustNShield
         |library = ${config.pkcs11LibraryPath}
         |slot = ${config.slotId}
         |""".stripMargin
    val sunPkcs11 = Security.getProvider("SunPKCS11")
    val configured = sunPkcs11.configure("--" + pkcs11Config) // config string form varies by JDK version
    Security.addProvider(configured)
    configured
  }

  private val keyStore: KeyStore = {
    val ks = KeyStore.getInstance("PKCS11", provider)
    ks.load(null, config.hsmPin.toCharArray)
    ks
  }

  // --- Key generation ---------------------------------------------------

  def generateSigningKeyPair(keyId: String, algorithm: SigningKeyAlgorithm): Future[Unit] =
    Future {
      val alg = algorithm match {
        case SigningKeyAlgorithm.Ed25519 => "Ed25519"
        case SigningKeyAlgorithm.EcDsaP256 => "EC" // with P-256 params set below
        case SigningKeyAlgorithm.EcDsaP384 => "EC"
      }
      val gen = KeyPairGenerator.getInstance(alg, provider)
      algorithm match {
        case SigningKeyAlgorithm.EcDsaP256 => gen.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"))
        case SigningKeyAlgorithm.EcDsaP384 => gen.initialize(new java.security.spec.ECGenParameterSpec("secp384r1"))
        case SigningKeyAlgorithm.Ed25519 => () // no params needed
      }
      val pair = gen.generateKeyPair()
      storeUnderAlias(keyId, pair) // TODO: confirm Entrust's actual PKCS#11 aliasing/labeling convention
    }

  def generateEncryptionKeyPair(keyId: String, algorithm: EncryptionKeyAlgorithm): Future[Unit] =
    Future {
      val (alg, initParam: Option[AlgorithmParameterSpec]) = algorithm match {
        case EncryptionKeyAlgorithm.EciesP256 => ("EC", Some(new java.security.spec.ECGenParameterSpec("secp256r1")))
        case EncryptionKeyAlgorithm.RsaOaep2048 => ("RSA", None)
      }
      val gen = KeyPairGenerator.getInstance(alg, provider)
      initParam.foreach(gen.initialize)
      if (initParam.isEmpty) gen.initialize(2048)
      val pair = gen.generateKeyPair()
      storeUnderAlias(keyId, pair)
    }

  def generateSymmetricKey(keyId: String): Future[Unit] =
    Future {
      val gen = javax.crypto.KeyGenerator.getInstance("AES", provider)
      gen.init(256)
      val key = gen.generateKey()
      keyStore.setKeyEntry(keyId, key, null, null)
    }

  // --- Cryptographic operations ------------------------------------------

  def sign(keyId: String, data: Array[Byte], algorithm: SigningAlgorithm): Future[Array[Byte]] =
    Future {
      val sigAlg = algorithm match {
        case SigningAlgorithm.Ed25519 => "Ed25519"
        case SigningAlgorithm.EcDsaSha256 => "SHA256withECDSA"
        case SigningAlgorithm.EcDsaSha384 => "SHA384withECDSA"
      }
      val privateKey = keyStore.getKey(keyId, config.hsmPin.toCharArray).asInstanceOf[PrivateKey]
      val sig = Signature.getInstance(sigAlg, provider)
      sig.initSign(privateKey)
      sig.update(data)
      sig.sign()
    }

  def decryptAsymmetric(keyId: String, ciphertext: Array[Byte], algorithm: EncryptionAlgorithm): Future[Array[Byte]] =
    Future {
      val cipherAlg = algorithm match {
        case EncryptionAlgorithm.RsaOaepSha256 => "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        case EncryptionAlgorithm.EciesHmacSha256Aes128Cbc => "ECIES" // TODO: confirm Entrust's ECIES transformation string, or decompose into ECDH + AES locally
      }
      val privateKey = keyStore.getKey(keyId, config.hsmPin.toCharArray).asInstanceOf[PrivateKey]
      val cipher = Cipher.getInstance(cipherAlg, provider)
      cipher.init(Cipher.DECRYPT_MODE, privateKey)
      cipher.doFinal(ciphertext)
    }

  def encryptSymmetric(keyId: String, plaintext: Array[Byte]): Future[Array[Byte]] =
    Future {
      val key = keyStore.getKey(keyId, config.hsmPin.toCharArray)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding", provider)
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val iv = cipher.getIV
      iv ++ cipher.doFinal(plaintext) // adjust to whatever Canton's symmetric-encryption contract expects
    }

  def decryptSymmetric(keyId: String, ciphertext: Array[Byte]): Future[Array[Byte]] =
    Future {
      val key = keyStore.getKey(keyId, config.hsmPin.toCharArray)
      val (iv, ct) = ciphertext.splitAt(12)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding", provider)
      cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(128, iv))
      cipher.doFinal(ct)
    }

  // --- Key management -----------------------------------------------------

  def getPublicKey(keyId: String): Future[Array[Byte]] =
    Future {
      keyStore.getCertificate(keyId).getPublicKey.getEncoded
    }

  def keyExists(keyId: String): Future[Boolean] =
    Future(keyStore.containsAlias(keyId))

  def deleteKey(keyId: String): Future[Unit] =
    Future(keyStore.deleteEntry(keyId))

  def health(): Future[KmsDriverHealth] =
    Future {
      try {
        keyStore.aliases() // cheap call to confirm the PKCS#11 session is alive
        KmsDriverHealth.Ok
      } catch {
        case e: Exception => KmsDriverHealth.Degraded(e.getMessage)
      }
    }

  private def storeUnderAlias(keyId: String, pair: KeyPair): Unit = {
    // TODO: PKCS#11 keystores generally require a certificate chain to store
    // a PrivateKeyEntry — check Entrust's Java integration guide for a more
    // direct "generate and label" API.
    throw new UnsupportedOperationException(
      "Reconcile against Entrust's actual key-labeling API before this is functional")
  }
}

// TODO: replace with the real case classes/enums from
// com.digitalasset.canton.crypto.kms.driver.api once that dependency is wired up.
sealed trait SigningKeyAlgorithm
object SigningKeyAlgorithm {
  case object Ed25519 extends SigningKeyAlgorithm
  case object EcDsaP256 extends SigningKeyAlgorithm
  case object EcDsaP384 extends SigningKeyAlgorithm
}

sealed trait EncryptionKeyAlgorithm
object EncryptionKeyAlgorithm {
  case object EciesP256 extends EncryptionKeyAlgorithm
  case object RsaOaep2048 extends EncryptionKeyAlgorithm
}

sealed trait SigningAlgorithm
object SigningAlgorithm {
  case object Ed25519 extends SigningAlgorithm
  case object EcDsaSha256 extends SigningAlgorithm
  case object EcDsaSha384 extends SigningAlgorithm
}

sealed trait EncryptionAlgorithm
object EncryptionAlgorithm {
  case object RsaOaepSha256 extends EncryptionAlgorithm
  case object EciesHmacSha256Aes128Cbc extends EncryptionAlgorithm
}

sealed trait KmsDriverHealth
object KmsDriverHealth {
  case object Ok extends KmsDriverHealth
  final case class Degraded(reason: String) extends KmsDriverHealth
}
