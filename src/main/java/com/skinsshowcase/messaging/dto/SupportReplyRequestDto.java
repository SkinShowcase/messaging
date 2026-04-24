package com.skinsshowcase.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportReplyRequestDto(
        @NotBlank(message = "recipientSteamId required")
        @Size(min = 17, max = 17)
        String recipientSteamId,
        @NotBlank(message = "text required")
        @Size(min = 1, max = 4096)
        String text
) {
}
