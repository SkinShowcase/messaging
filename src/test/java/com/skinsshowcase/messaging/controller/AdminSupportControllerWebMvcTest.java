package com.skinsshowcase.messaging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skinsshowcase.messaging.config.AdminApiProperties;
import com.skinsshowcase.messaging.dto.MessageResponseDto;
import com.skinsshowcase.messaging.dto.SupportReplyRequestDto;
import com.skinsshowcase.messaging.service.MessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSupportControllerWebMvcTest {

    @Mock
    AdminApiProperties adminApiProperties;

    @Mock
    MessagingService messagingService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminSupportController(adminApiProperties, messagingService))
                .build();
    }

    @Test
    void listIncoming_ok() throws Exception {
        when(adminApiProperties.getApiKey()).thenReturn("k");
        when(messagingService.listIncomingToSupport(0, 50)).thenReturn(
                new com.skinsshowcase.messaging.dto.AdminSupportInboxPageDto(List.of(), 0, 50, 0, 0));

        mockMvc.perform(get("/api/admin/messaging/support/incoming").header("X-Admin-Api-Key", "k"))
                .andExpect(status().isOk());
    }

    @Test
    void sendSupport_created() throws Exception {
        when(adminApiProperties.getApiKey()).thenReturn("k");
        var id = UUID.randomUUID();
        var resp = new MessageResponseDto(id, "0".repeat(17), "76561198000000002", "t", Instant.now());
        when(messagingService.sendSupportMessageToUser(anyString(), anyString())).thenReturn(resp);

        mockMvc.perform(post("/api/admin/messaging/support/messages")
                        .header("X-Admin-Api-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SupportReplyRequestDto("76561198000000002", "hello"))))
                .andExpect(status().isCreated());

        verify(messagingService).sendSupportMessageToUser(eq("76561198000000002"), eq("hello"));
    }

    @Test
    void unauthorized_wrongKey() throws Exception {
        when(adminApiProperties.getApiKey()).thenReturn("k");
        mockMvc.perform(get("/api/admin/messaging/support/incoming").header("X-Admin-Api-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }
}
