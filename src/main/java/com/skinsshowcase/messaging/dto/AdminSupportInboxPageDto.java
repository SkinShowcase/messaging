package com.skinsshowcase.messaging.dto;

import java.util.List;

/**
 * Входящие сообщения для синтетической поддержки (получатель — {@code SupportSyntheticSteamId#VALUE}).
 */
public record AdminSupportInboxPageDto(
        List<MessageResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
