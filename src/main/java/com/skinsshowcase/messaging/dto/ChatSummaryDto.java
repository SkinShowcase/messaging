package com.skinsshowcase.messaging.dto;

import java.time.Instant;

/**
 * Сводка чата. {@code support==true} — чат с поддержкой (синтетический Steam ID {@code 0}×17).
 * {@code counterpartyPresetAvatarId} — id пресета (1–8), если у собеседника выбран PRESET; иначе null.
 */
public record ChatSummaryDto(
        String counterpartySteamId,
        String lastMessagePreview,
        Instant lastMessageAt,
        boolean support,
        Integer counterpartyPresetAvatarId
) {
}
