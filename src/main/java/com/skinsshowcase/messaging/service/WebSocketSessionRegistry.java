package com.skinsshowcase.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skinsshowcase.messaging.dto.MessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр WebSocket-сессий по steamId. Потокобезопасный.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionRegistry {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessionsBySteamId = new ConcurrentHashMap<>();

    public void register(String steamId, WebSocketSession session) {
        var previous = sessionsBySteamId.put(steamId, session);
        if (previous != null && previous.isOpen()) {
            closeQuietly(previous);
        }
    }

    public void unregister(String steamId) {
        sessionsBySteamId.remove(steamId);
    }

    public void sendToUser(String recipientSteamId, MessageResponseDto message) {
        var session = sessionsBySteamId.get(recipientSteamId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            var json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize message for WebSocket: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message: {}", e.getMessage());
        }
    }

    private static void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException e) {
            log.debug("Error closing previous session: {}", e.getMessage());
        }
    }
}
