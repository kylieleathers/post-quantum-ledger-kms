package com.corekms.kms;

import java.security.GeneralSecurityException;
import java.util.Properties;

public final class KmsDriverFactory {

    private KmsDriverFactory() {}

    public static KmsDriver create(Properties config) throws GeneralSecurityException, java.io.IOException {
        String provider = config.getProperty("kms.provider", "software");
        return switch (provider) {
            case "software" -> new SoftwareKmsDriver();
            case "entrust" -> new EntrustHsmKmsDriver(
                    config.getProperty("kms.entrust.pkcs11LibraryPath"),
                    Integer.parseInt(config.getProperty("kms.entrust.slotId", "0")),
                    config.getProperty("kms.entrust.hsmPin"));
            default -> throw new IllegalArgumentException("Unknown kms.provider: " + provider);
        };
    }
}
