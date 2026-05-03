package com.skinsshowcase.messaging.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageNotificationServiceTest {

    @Mock
    WebSocketSessionRegistry sessionRegistry;

    @Mock
    TcpNotificationServer tcpNotificationServer;

    @InjectMocks
    MessageNotificationService notificationService;

    @Test
    void notifyNewMessage_sendsToSocketAndTcp() {
        var id = UUID.randomUUID();
        notificationService.notifyNewMessage("76561198000000001", "76561198000000002", id, "hi");

        verify(sessionRegistry).sendToUser(eq("76561198000000002"), any());
        verify(tcpNotificationServer).notifyNewMessage(eq("76561198000000002"), eq("76561198000000001"), eq(id));
    }
}
