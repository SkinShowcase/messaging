package com.skinsshowcase.messaging.service;

import com.skinsshowcase.messaging.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Валидация JWT и извлечение subject (SteamID64). Секрет тот же, что в auth.
 */
@Service
public class JwtSupportService {

    private static final Logger log = LoggerFactory.getLogger(JwtSupportService.class);
    private static final int MIN_SECRET_BYTES_HS256 = 32;

    private final SecretKey signingKey;

    public JwtSupportService(JwtProperties properties) {
        this.signingKey = secretKeyFromString(properties.getSecret());
    }

    /**
     * Парсит JWT и возвращает subject (SteamID64).
     *
     * @return SteamID64 при валидном токене
     * @throws JwtException при истёкшем или невалидном токене
     */
    public String parseSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private static SecretKey secretKeyFromString(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be empty");
        }
        var bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES_HS256) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least " + MIN_SECRET_BYTES_HS256 + " bytes for HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
