package com.skinsshowcase.messaging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skinsshowcase.messaging.dto.ChatSummaryDto;
import com.skinsshowcase.messaging.dto.MessageResponseDto;
import com.skinsshowcase.messaging.dto.SendMessageRequestDto;
import com.skinsshowcase.messaging.resolver.CurrentUserArgumentResolver;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessagingControllerWebMvcTest {

    private static final String A = "76561198000000001";
    private static final String B = "76561198000000002";

    @Mock
    MessagingService messagingService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        var controller = new MessagingController(messagingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @Test
    void sendMessage_created() throws Exception {
        var id = UUID.randomUUID();
        var dto = new MessageResponseDto(id, A, B, "hi", Instant.parse("2026-01-01T00:00:00Z"));
        when(messagingService.sendMessage(eq(A), eq(B), any(SendMessageRequestDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/chats/{rid}/messages", B)
                        .requestAttr(CurrentUserArgumentResolver.STEAM_ID_ATTRIBUTE, A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequestDto("hi"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("hi"));
    }

    @Test
    void getChatHistory_ok() throws Exception {
        var id = UUID.randomUUID();
        var msg = new MessageResponseDto(id, A, B, "x", Instant.now());
        when(messagingService.getChatHistory(A, B, 0, 50)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/chats/{cid}/messages", B)
                        .requestAttr(CurrentUserArgumentResolver.STEAM_ID_ATTRIBUTE, A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("x"));
    }

    @Test
    void deleteMessage_noContent() throws Exception {
        var id = UUID.randomUUID();
        mockMvc.perform(delete("/api/chats/{cid}/messages/{mid}", B, id)
                        .requestAttr(CurrentUserArgumentResolver.STEAM_ID_ATTRIBUTE, A))
                .andExpect(status().isNoContent());
        verify(messagingService).deleteMessage(A, B, id);
    }

    @Test
    void getChats_ok() throws Exception {
        when(messagingService.getChats(eq(A), eq("Bearer t")))
                .thenReturn(List.of(new ChatSummaryDto(B, "p", Instant.now(), false, 1)));

        mockMvc.perform(get("/api/chats")
                        .requestAttr(CurrentUserArgumentResolver.STEAM_ID_ATTRIBUTE, A)
                        .header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].counterpartySteamId").value(B));
    }

    @Test
    void getChatByUsername_ok() throws Exception {
        when(messagingService.getChatByUsername(eq(A), eq("bob"), any()))
                .thenReturn(new ChatSummaryDto(B, "", Instant.EPOCH, false, null));

        mockMvc.perform(get("/api/chats/by-username/{u}", "bob")
                        .requestAttr(CurrentUserArgumentResolver.STEAM_ID_ATTRIBUTE, A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterpartySteamId").value(B));
    }
}
