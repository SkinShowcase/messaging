package com.skinsshowcase.messaging.client;

import com.skinsshowcase.messaging.dto.AuthUserSteamIdDto;
import com.skinsshowcase.messaging.dto.BatchPresetAvatarIdsResponseDto;
import com.skinsshowcase.messaging.dto.BatchSteamIdsRequestDto;
import com.skinsshowcase.messaging.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Клиент к сервису auth: резолв имени пользователя в Steam ID.
 */
@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);
    private static final String BY_USERNAME_PATH = "/auth/users/by-username/{username}";
    private static final String PRESET_AVATAR_IDS_PATH = "/auth/users/preset-avatar-ids";

    private final WebClient webClient;

    public AuthClient(@Qualifier("authWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Возвращает Steam ID пользователя по имени (Steam ID или persona name).
     *
     * @param username         имя или Steam ID
     * @param authorizationHeader значение заголовка Authorization (Bearer JWT)
     * @return Steam ID или empty, если пользователь не найден
     */
    public Optional<String> getSteamIdByUsername(String username, String authorizationHeader) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        try {
            var spec = webClient.get()
                    .uri(BY_USERNAME_PATH, username);
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            var dto = spec.retrieve()
                    .bodyToMono(AuthUserSteamIdDto.class)
                    .block();
            if (dto == null || dto.steamId() == null || dto.steamId().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(dto.steamId());
        } catch (WebClientResponseException.NotFound e) {
            log.debug("User not found by username: {}", username);
            return Optional.empty();
        } catch (WebClientResponseException e) {
            log.warn("Auth service returned {} for username {}: {}", e.getStatusCode(), username, e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Auth client error for username {}: {}", username, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Id пресетной аватарки (1–8) по Steam ID; null в значении — пользователь не найден.
     */
    public Map<String, Integer> batchPresetAvatarIds(List<String> steamIds, String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Collections.emptyMap();
        }
        if (steamIds == null || steamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            var dto = webClient.post()
                    .uri(PRESET_AVATAR_IDS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .bodyValue(new BatchSteamIdsRequestDto(steamIds))
                    .retrieve()
                    .bodyToMono(BatchPresetAvatarIdsResponseDto.class)
                    .block();
            return extractPresetMap(dto);
        } catch (WebClientResponseException e) {
            log.warn("Auth batch preset-avatar returned {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Auth batch preset-avatar error: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static Map<String, Integer> extractPresetMap(BatchPresetAvatarIdsResponseDto dto) {
        if (dto == null) {
            return Collections.emptyMap();
        }
        if (dto.presetAvatarIdBySteamId() == null) {
            return Collections.emptyMap();
        }
        return new LinkedHashMap<>(dto.presetAvatarIdBySteamId());
    }
}
