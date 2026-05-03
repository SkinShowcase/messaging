package com.skinsshowcase.messaging.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesBeansTest {

    @Test
    void jwtProperties_roundTrip() {
        var p = new JwtProperties();
        p.setSecret("0123456789abcdef0123456789abcdef");
        p.setExpirationMs(60_000L);
        assertThat(p.getSecret()).hasSize(32);
        assertThat(p.getExpirationMs()).isEqualTo(60_000L);
    }

    @Test
    void messageCryptoProperties_key() {
        var p = new MessageCryptoProperties();
        p.setKey("k");
        assertThat(p.getKey()).isEqualTo("k");
    }

    @Test
    void adminApiProperties_nullKeyBecomesEmpty() {
        var p = new AdminApiProperties();
        p.setApiKey(null);
        assertThat(p.getApiKey()).isEmpty();
    }
}
