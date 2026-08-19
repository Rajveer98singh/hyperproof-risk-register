package com.hyperproof.riskregister.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the configured encryption passphrase into {@link EncryptionKeyHolder} at startup so the
 * JPA converter uses the environment-provided key rather than the lazy default.
 */
@Configuration
public class EncryptionConfig {

    public EncryptionConfig(@Value("${risk.encryption.key}") String passphrase) {
        EncryptionKeyHolder.configure(passphrase);
    }
}
