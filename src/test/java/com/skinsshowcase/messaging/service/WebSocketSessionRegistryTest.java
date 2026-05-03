package com.skinsshowcase.messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skinsshowcase.messaging.dto.MessageResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionRegistryTest {

    @Mock
    WebSocketSession session;

    WebSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        registry = new WebSocketSessionRegistry(mapper);
    }

    @Test
    void sendToUser_whenOpen_sendsJson() throws java.io.IOException {
        when(session.isOpen()).thenReturn(true);
        registry.register("76561198000000002", session);

        var id = UUID.randomUUID();
        var dto = new MessageResponseDto(id, "76561198000000001", "76561198000000002", "t", Instant.now());
        registry.sendToUser("76561198000000002", dto);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToUser_noSession_noSend() throws java.io.IOException {
        var id = UUID.randomUUID();
        var dto = new MessageResponseDto(id, "76561198000000001", "76561198000000002", "t", Instant.now());
        registry.sendToUser("76561198000000002", dto);

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void unregister_removes() throws java.io.IOException {
        registry.register("76561198000000001", session);
        registry.unregister("76561198000000001");
        registry.sendToUser("76561198000000001", new MessageResponseDto(
                UUID.randomUUID(), "a", "b", "x", Instant.now()));
        verify(session, never()).sendMessage(any(TextMessage.class));
    }
}
