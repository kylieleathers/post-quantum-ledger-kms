package com.canton.kms.entrust

import com.digitalasset.canton.crypto.kms.driver.api.v1._
import pureconfig.{ConfigReader, ConfigWriter}

import scala.concurrent.ExecutionContext

/**
 * Entry point Canton discovers via META-INF/services. Canton instantiates
 * this once per configured driver and calls create() to get a running
 * EntrustKmsDriver instance.
 *
 * Base trait/method names are written from Digital Asset's documented
 * description of the KMS Driver API, not from the actual
 * canton-kms-driver-api source — confirm against that artifact (see
 * build.sbt) before this is expected to compile as-is.
 */
class EntrustKmsDriverFactory extends DriverFactory {

  override type ConfigType = EntrustKmsDriverConfig

  override def name: String = "entrust-nshield"

  override def version: Int = 1

  override def buildInfo: Option[String] = Some("canton-entrust-kms-driver 0.1.0")

  override def configReader: ConfigReader[EntrustKmsDriverConfig] =
    EntrustKmsDriverConfig.reader

  override def configWriter(confidential: Boolean): ConfigWriter[EntrustKmsDriverConfig] =
    EntrustKmsDriverConfig.writer(confidential)

  override def create(
      config: EntrustKmsDriverConfig,
      loggerFactory: Any, // TODO: actual type is Canton's NamedLoggerFactory
      executionContext: ExecutionContext
  ): Driver = new EntrustKmsDriver(config)(executionContext)
}

/**
 * Driver-specific configuration, surfaced in canton.conf under
 * canton.participants.<name>.crypto.kms.config (see conf/canton-entrust-kms.conf.example).
 */
final case class EntrustKmsDriverConfig(
    pkcs11LibraryPath: String, // path to Entrust's PKCS#11 module (e.g. libcknfast.so)
    slotId: Int,
    hsmPin: String // source from an env var or external secret, not a plaintext conf value
)

object EntrustKmsDriverConfig {
  import pureconfig.generic.semiauto._

  val reader: ConfigReader[EntrustKmsDriverConfig] = deriveReader[EntrustKmsDriverConfig]

  def writer(confidential: Boolean): ConfigWriter[EntrustKmsDriverConfig] = {
    val base = deriveWriter[EntrustKmsDriverConfig]
    if (confidential) base.contramap(c => c.copy(hsmPin = "****")) else base
  }
}
