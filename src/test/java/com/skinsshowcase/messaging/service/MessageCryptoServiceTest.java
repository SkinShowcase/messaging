package com.skinsshowcase.messaging.service;

import com.skinsshowcase.messaging.config.MessageCryptoProperties;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageCryptoServiceTest {

    private static MessageCryptoService serviceWithKey(String base64Key) {
        var props = new MessageCryptoProperties();
        props.setKey(base64Key);
        return new MessageCryptoService(props);
    }

    private static String randomKey32Base64() {
        var key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Test
    void encryptDecrypt_roundTrip() {
        var svc = serviceWithKey(randomKey32Base64());
        var plain = "Hello, support chat";

        assertThat(svc.decrypt(svc.encrypt(plain))).isEqualTo(plain);
    }

    @Test
    void encrypt_nullOrEmpty_returnsAsIs() {
        var svc = serviceWithKey(randomKey32Base64());

        assertThat(svc.encrypt(null)).isNull();
        assertThat(svc.encrypt("")).isEmpty();

        assertThat(svc.decrypt(null)).isNull();
        assertThat(svc.decrypt("")).isEmpty();
    }

    @Test
    void constructor_rejectsMissingOrInvalidKey() {
        var empty = new MessageCryptoProperties();
        empty.setKey("");
        assertThatThrownBy(() -> new MessageCryptoService(empty))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("message-crypto.key");

        var shortKey = new MessageCryptoProperties();
        shortKey.setKey(Base64.getEncoder().encodeToString(new byte[16]));
        assertThatThrownBy(() -> new MessageCryptoService(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32-byte");
    }

    @Test
    void decrypt_plaintextLegacy_returnsSameString() {
        var svc = serviceWithKey(randomKey32Base64());
        var legacy = "not-encrypted-message";

        assertThat(svc.decrypt(legacy)).isEqualTo(legacy);
    }
}
