package com.skinsshowcase.messaging.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
public class AuthProperties {

    private final String baseUrl;
    private final long connectTimeoutMs;
    private final long readTimeoutMs;

    public AuthProperties(
            @NotBlank @Value("${app.auth.service.base-url}") String baseUrl,
            @Positive @Value("${app.auth.service.connect-timeout-ms}") long connectTimeoutMs,
            @Positive @Value("${app.auth.service.read-timeout-ms}") long readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }
}
