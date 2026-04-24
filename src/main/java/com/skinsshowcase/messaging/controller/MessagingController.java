package com.skinsshowcase.messaging.controller;

import com.skinsshowcase.messaging.dto.ChatSummaryDto;
import com.skinsshowcase.messaging.dto.MessageResponseDto;
import com.skinsshowcase.messaging.dto.SendMessageRequestDto;
import com.skinsshowcase.messaging.resolver.CurrentUser;
import com.skinsshowcase.messaging.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Messaging", description = "Отправка сообщений, история чата, список чатов")
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Operation(summary = "Отправить сообщение", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/chats/{recipientSteamId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponseDto sendMessage(@CurrentUser String senderSteamId,
                                          @PathVariable String recipientSteamId,
                                          @Valid @RequestBody SendMessageRequestDto body) {
        return messagingService.sendMessage(senderSteamId, recipientSteamId, body);
    }

    @Operation(summary = "История чата с пользователем", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/chats/{counterpartySteamId}/messages")
    public List<MessageResponseDto> getChatHistory(@CurrentUser String userSteamId,
                                                    @PathVariable String counterpartySteamId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        return messagingService.getChatHistory(userSteamId, counterpartySteamId, page, size);
    }

    @Operation(summary = "Удалить сообщение", description = "Только участник чата; сообщение не старше 24 часов",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/chats/{counterpartySteamId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@CurrentUser String userSteamId,
                              @PathVariable String counterpartySteamId,
                              @PathVariable UUID messageId) {
        messagingService.deleteMessage(userSteamId, counterpartySteamId, messageId);
    }

    @Operation(summary = "Список всех чатов пользователя", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/chats")
    public List<ChatSummaryDto> getChats(@CurrentUser String userSteamId,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return messagingService.getChats(userSteamId, authorization);
    }

    @Operation(summary = "Найти чат по имени пользователя", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/chats/by-username/{username}")
    public ChatSummaryDto getChatByUsername(@CurrentUser String userSteamId,
                                             @PathVariable String username,
                                             @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return messagingService.getChatByUsername(userSteamId, username, authorization);
    }
}
