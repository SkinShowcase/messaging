package com.skinsshowcase.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequestDto(
        @NotBlank(message = "text must not be blank")
        @Size(min = 1, max = 4096)
        String text
) {
}
