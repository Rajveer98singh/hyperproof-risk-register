package com.hyperproof.riskregister.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Holds the AES key used by {@link EncryptedStringConverter}.
 *
 * <p>JPA attribute converters are instantiated by Hibernate (not Spring), so they cannot use
 * constructor injection reliably across every test slice. This holder is the single source of the
 * key: {@code EncryptionConfig} pushes the resolved {@code risk.encryption.key} property in at
 * application startup, while a lazy default (env {@code RISK_ENC_KEY}, else a documented dev
 * fallback) keeps repository slice tests working without the full context.
 *
 * <p>The passphrase is stretched to a 256-bit AES key via SHA-256. A production system would use a
 * KDF (e.g. PBKDF2/Argon2) with a salt and a key from a secrets manager — noted in the README.
 */
public final class EncryptionKeyHolder {

    private static volatile SecretKey key = deriveKey(resolveDefaultPassphrase());

    private EncryptionKeyHolder() {
    }

    public static SecretKey key() {
        return key;
    }

    /** Called by {@code EncryptionConfig} at startup with the Spring-resolved property value. */
    static void configure(String passphrase) {
        key = deriveKey(passphrase);
    }

    private static String resolveDefaultPassphrase() {
        String env = System.getenv("RISK_ENC_KEY");
        return env != null && !env.isBlank() ? env : "dev-only-insecure-key-change-me";
    }

    private static SecretKey deriveKey(String passphrase) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(passphrase.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
