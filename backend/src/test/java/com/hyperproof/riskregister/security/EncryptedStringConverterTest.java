package com.hyperproof.riskregister.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EncryptedStringConverterTest {

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @Test
    void roundTripsPlaintext() {
        String plaintext = "Jane Doe, Head of Security";
        String stored = converter.convertToDatabaseColumn(plaintext);
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(plaintext);
    }

    @Test
    void storedValueIsNotPlaintext() {
        String plaintext = "sensitive-owner";
        String stored = converter.convertToDatabaseColumn(plaintext);
        assertThat(stored).doesNotContain(plaintext);
    }

    @Test
    void producesDifferentCiphertextEachTime_randomIv() {
        String plaintext = "same input";
        assertThat(converter.convertToDatabaseColumn(plaintext))
                .isNotEqualTo(converter.convertToDatabaseColumn(plaintext));
    }

    @Test
    void handlesNulls() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void tamperedCiphertextFailsToDecrypt() {
        String stored = converter.convertToDatabaseColumn("integrity-protected");
        // Flip the last character (GCM auth tag) -> decryption must fail rather than return garbage.
        String tampered = stored.substring(0, stored.length() - 2)
                + (stored.endsWith("A") ? "B" : "A") + "=";
        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class);
    }
}
