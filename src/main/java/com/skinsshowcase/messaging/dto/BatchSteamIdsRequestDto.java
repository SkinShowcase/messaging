package com.skinsshowcase.messaging.dto;

import java.util.List;

/**
 * Тело POST /auth/users/preset-avatar-ids (сериализация как в auth).
 */
public record BatchSteamIdsRequestDto(List<String> steamIds) {
}
