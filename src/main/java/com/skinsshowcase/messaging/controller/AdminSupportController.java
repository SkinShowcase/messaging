package com.skinsshowcase.messaging.controller;

import com.skinsshowcase.messaging.config.AdminApiProperties;
import com.skinsshowcase.messaging.dto.AdminSupportInboxPageDto;
import com.skinsshowcase.messaging.dto.MessageResponseDto;
import com.skinsshowcase.messaging.dto.SupportReplyRequestDto;
import com.skinsshowcase.messaging.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Отправка сообщения пользователю от имени поддержки (синтетический отправитель — 17 нулей).
 */
@Tag(name = "Admin messaging", description = "Сообщения от поддержки по API-ключу")
@RestController
@RequestMapping(path = "/api/admin/messaging", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminSupportController {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Api-Key";

    private final AdminApiProperties adminApiProperties;
    private final MessagingService messagingService;

    public AdminSupportController(AdminApiProperties adminApiProperties, MessagingService messagingService) {
        this.adminApiProperties = adminApiProperties;
        this.messagingService = messagingService;
    }

    @Operation(summary = "Входящие сообщения пользователей поддержке")
    @GetMapping("/support/incoming")
    public AdminSupportInboxPageDto listIncomingToSupport(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        requireValidAdminKey(apiKey);
        return messagingService.listIncomingToSupport(page, size);
    }

    @Operation(summary = "Отправить сообщение от поддержки пользователю")
    @PostMapping(value = "/support/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponseDto sendSupportMessage(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String apiKey,
            @Valid @RequestBody SupportReplyRequestDto body) {
        requireValidAdminKey(apiKey);
        return messagingService.sendSupportMessageToUser(body.recipientSteamId(), body.text());
    }

    private void requireValidAdminKey(String providedHeader) {
        var configured = adminApiProperties.getApiKey();
        if (configured == null || configured.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Admin API is not configured");
        }
        if (!constantTimeEqual(configured, providedHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin API key");
        }
    }

    private static boolean constantTimeEqual(String expected, String provided) {
        var a = expected.getBytes(StandardCharsets.UTF_8);
        var b = (provided == null ? "" : provided).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
