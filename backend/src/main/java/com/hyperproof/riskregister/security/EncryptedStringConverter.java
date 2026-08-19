package com.hyperproof.riskregister.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Transparently encrypts a String column at rest using AES-256-GCM.
 *
 * <p>Applied to PII-ish fields ({@code owner}, {@code description}). GCM provides both
 * confidentiality and integrity (tampered ciphertext fails to decrypt). A fresh random 12-byte IV
 * is generated per value and prepended to the ciphertext; the whole blob is Base64-encoded for
 * storage in a TEXT column.
 *
 * <p>Trade-off (documented in README): encrypted columns are opaque to SQL, so they cannot be
 * filtered/sorted in the database. That is fine here because all filtering is on the unencrypted
 * enum columns (category, status).
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;        // 96-bit nonce, recommended for GCM
    private static final int TAG_LENGTH_BITS = 128; // authentication tag
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, EncryptionKeyHolder.key(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV so we can decrypt later: [ IV | ciphertext+tag ]
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt column value", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, EncryptionKeyHolder.key(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt column value", e);
        }
    }
}
