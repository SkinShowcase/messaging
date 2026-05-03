package com.skinsshowcase.messaging.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SteamIdHashingServiceTest {

    private final SteamIdHashingService hashing = new SteamIdHashingService();

    @Test
    void sha256_nullOrBlank_returnsNull() {
        assertThat(hashing.sha256(null)).isNull();
        assertThat(hashing.sha256("")).isNull();
        assertThat(hashing.sha256("   ")).isNull();
    }

    @Test
    void sha256_trimsAndMatchesKnownVector() {
        assertThat(hashing.sha256("hello")).isEqualTo(hashing.sha256("  hello  "));
        assertThat(hashing.sha256("test"))
                .isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    }
}
