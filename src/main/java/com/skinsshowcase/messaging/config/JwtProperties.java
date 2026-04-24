package com.skinsshowcase.messaging.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.auth.jwt")
@Validated
public class JwtProperties {

    @NotBlank(message = "app.auth.jwt.secret / AUTH_JWT_SECRET must be set")
    private String secret;

    @Positive
    private long expirationMs = 86400_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
