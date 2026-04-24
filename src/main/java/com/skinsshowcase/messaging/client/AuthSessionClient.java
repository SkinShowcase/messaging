package com.skinsshowcase.messaging.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Проверка активной сессии в auth ({@code GET /auth/session}): JWT и отсутствие блокировки.
 */
@Component
public class AuthSessionClient {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionClient.class);
    private static final String SESSION_PATH = "/auth/session";

    private final WebClient webClient;

    public AuthSessionClient(@Qualifier("authWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * @param authorizationHeader полный заголовок {@code Authorization} (включая {@code Bearer })
     */
    public AuthSessionStatus checkSession(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return AuthSessionStatus.UNAUTHORIZED;
        }
        try {
            var response = webClient.get()
                    .uri(SESSION_PATH)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            if (response == null) {
                return AuthSessionStatus.ERROR;
            }
            if (response.getStatusCode().value() == 204) {
                return AuthSessionStatus.OK;
            }
            return AuthSessionStatus.ERROR;
        } catch (WebClientResponseException.Unauthorized e) {
            return AuthSessionStatus.UNAUTHORIZED;
        } catch (WebClientResponseException.Forbidden e) {
            return AuthSessionStatus.BLOCKED;
        } catch (WebClientResponseException e) {
            log.warn("Auth session check HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return AuthSessionStatus.ERROR;
        } catch (Exception e) {
            log.warn("Auth session check failed: {}", e.getMessage());
            return AuthSessionStatus.ERROR;
        }
    }

    public enum AuthSessionStatus {
        OK,
        UNAUTHORIZED,
        BLOCKED,
        ERROR
    }
}
