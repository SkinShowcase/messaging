package com.skinsshowcase.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponseDto(
        UUID id,
        String senderSteamId,
        String recipientSteamId,
        String text,
        Instant createdAt
) {
}
