package com.skinsshowcase.messaging.service;

import com.skinsshowcase.messaging.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSupportServiceTest {

    private static final String SECRET_32 = "0123456789abcdef0123456789abcdef";

    private static JwtSupportService service() {
        var p = new JwtProperties();
        p.setSecret(SECRET_32);
        return new JwtSupportService(p);
    }

    private static String createToken(String subject, long ttlMs) {
        var key = Keys.hmacShaKeyFor(SECRET_32.getBytes(StandardCharsets.UTF_8));
        var now = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    @Test
    void parseSubject_roundTrip() {
        var svc = service();
        var token = createToken("76561198000000001", 60_000L);

        assertThat(svc.parseSubject(token)).isEqualTo("76561198000000001");
        assertThat(svc.isValid(token)).isTrue();
    }

    @Test
    void isValid_falseForNullBlankAndBadToken() {
        var svc = service();

        assertThat(svc.isValid(null)).isFalse();
        assertThat(svc.isValid("  ")).isFalse();
        assertThat(svc.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void parseSubject_throwsWhenExpired() throws InterruptedException {
        var svc = service();
        var token = createToken("76561198000000001", 1L);
        Thread.sleep(10);

        assertThatThrownBy(() -> svc.parseSubject(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_rejectsShortSecret() {
        var p = new JwtProperties();
        p.setSecret("short");

        assertThatThrownBy(() -> new JwtSupportService(p))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }
}
