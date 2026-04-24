package com.skinsshowcase.messaging.dto;

import java.util.Map;

/**
 * Ответ auth POST /auth/users/preset-avatar-ids.
 */
public record BatchPresetAvatarIdsResponseDto(Map<String, Integer> presetAvatarIdBySteamId) {
}
