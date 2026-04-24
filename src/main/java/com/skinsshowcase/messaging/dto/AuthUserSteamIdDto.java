package com.skinsshowcase.messaging.dto;

/**
 * Ответ auth GET /auth/users/by-username/{username}: Steam ID пользователя.
 */
public record AuthUserSteamIdDto(String steamId) {
}
