package com.skinsshowcase.messaging.service;

import com.skinsshowcase.messaging.dto.MessageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Рассылает уведомления о новом сообщении: WebSocket (полное сообщение) и TCP (сигнал о факте доставки).
 */
@Service
@RequiredArgsConstructor
public class MessageNotificationService {

    private final WebSocketSessionRegistry sessionRegistry;
    private final TcpNotificationServer tcpNotificationServer;

    public void notifyNewMessage(String senderSteamId, String recipientSteamId, UUID messageId, String text) {
        var dto = new MessageResponseDto(messageId, senderSteamId, recipientSteamId, text, Instant.now());
        sessionRegistry.sendToUser(recipientSteamId, dto);
        tcpNotificationServer.notifyNewMessage(recipientSteamId, senderSteamId, messageId);
    }
}
