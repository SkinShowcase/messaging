package com.skinsshowcase.messaging.service;

import com.skinsshowcase.messaging.client.AuthClient;
import com.skinsshowcase.messaging.config.SupportSyntheticSteamId;
import com.skinsshowcase.messaging.dto.SendMessageRequestDto;
import com.skinsshowcase.messaging.entity.Message;
import com.skinsshowcase.messaging.exception.UserNotFoundException;
import com.skinsshowcase.messaging.metrics.MessagingMetrics;
import com.skinsshowcase.messaging.repository.MessageRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessagingServiceTest {

    private static final String A = "76561198000000001";
    private static final String B = "76561198000000002";
    private static final String HA = "hash_a";
    private static final String HB = "hash_b";

    @Mock
    MessageRepository messageRepository;
    @Mock
    MessageNotificationService messageNotificationService;
    @Mock
    AuthClient authClient;
    @Mock
    MessageCryptoService messageCryptoService;
    @Mock
    SteamIdHashingService steamIdHashingService;

    private MessagingMetrics messagingMetrics;
    private MessagingService messagingService;

    @BeforeEach
    void setUp() {
        messagingMetrics = new MessagingMetrics(new SimpleMeterRegistry());
        messagingService = new MessagingService(
                messageRepository,
                messageNotificationService,
                authClient,
                messagingMetrics,
                messageCryptoService,
                steamIdHashingService);
        when(steamIdHashingService.sha256(eq(A))).thenReturn(HA);
        when(steamIdHashingService.sha256(eq(B))).thenReturn(HB);
        when(messageCryptoService.encrypt(any())).thenAnswer(inv -> "ENC:" + inv.getArgument(0));
        when(messageCryptoService.decrypt(any())).thenAnswer(inv -> {
            var s = (String) inv.getArgument(0);
            if (s != null && s.startsWith("ENC:")) {
                return s.substring(4);
            }
            return s;
        });
    }

    @Test
    void sendMessage_invalidRecipient() {
        assertThatThrownBy(() -> messagingService.sendMessage(A, "bad", new SendMessageRequestDto("x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendMessage_selfForbidden() {
        assertThatThrownBy(() -> messagingService.sendMessage(A, A, new SendMessageRequestDto("x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendMessage_persistsAndNotifies() {
        var dto = new SendMessageRequestDto("  hello  ");
        var out = messagingService.sendMessage(A, B, dto);

        assertThat(out.text()).isEqualTo("hello");
        verify(messageRepository).save(any(Message.class));
        verify(messageNotificationService).notifyNewMessage(eq(A), eq(B), any(UUID.class), eq("hello"));
    }

    @Test
    void sendSupportMessageToUser_delegatesToSend() {
        var out = messagingService.sendSupportMessageToUser(B, " hi ");

        assertThat(out.recipientSteamId()).isEqualTo(B);
        assertThat(out.text()).isEqualTo("hi");
    }

    @Test
    void listIncomingToSupport_mapsPage() {
        var id = UUID.randomUUID();
        var msg = new Message(id, HA, SupportSyntheticSteamId.VALUE,
                "ENC:" + A, "ENC:" + SupportSyntheticSteamId.RAW_VALUE, "ENC:txt", Instant.now());
        var page = new PageImpl<>(List.of(msg), PageRequest.of(0, 10), 1);
        when(messageRepository.findByRecipientIdOrderByCreatedAtDesc(eq(SupportSyntheticSteamId.VALUE), any()))
                .thenReturn(page);

        var inbox = messagingService.listIncomingToSupport(0, 10);

        assertThat(inbox.content()).hasSize(1);
        assertThat(inbox.content().get(0).text()).isEqualTo("txt");
    }

    @Test
    void getChatHistory_returnsDecrypted() {
        var id = UUID.randomUUID();
        var msg = new Message(id, HA, HB, "ENC:" + A, "ENC:" + B, "ENC:secret", Instant.now());
        when(messageRepository.findChatBetween(eq(HA), eq(HB), any())).thenReturn(List.of(msg));

        var history = messagingService.getChatHistory(A, B, 0, 50);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).text()).isEqualTo("secret");
        assertThat(history.get(0).senderSteamId()).isEqualTo(A);
    }

    @Test
    void getChatHistory_whenUserIsRecipient_mapsSenderAsCounterparty() {
        var id = UUID.randomUUID();
        var msg = new Message(id, HB, HA, "ENC:" + B, "ENC:" + A, "ENC:incoming", Instant.now());
        when(messageRepository.findChatBetween(eq(HA), eq(HB), any())).thenReturn(List.of(msg));

        var history = messagingService.getChatHistory(A, B, 0, 50);

        assertThat(history.get(0).senderSteamId()).isEqualTo(B);
        assertThat(history.get(0).text()).isEqualTo("incoming");
    }

    @Test
    void getChats_longPreview_truncated() {
        var longText = "x".repeat(100);
        var id = UUID.randomUUID();
        var msg = new Message(id, HA, HB, "ENC:" + A, "ENC:" + B, "ENC:" + longText, Instant.now());
        when(messageRepository.findBySenderIdOrRecipientIdOrderByCreatedAtDesc(eq(HA), eq(HA)))
                .thenReturn(List.of(msg));
        when(authClient.batchPresetAvatarIds(any(), any())).thenReturn(Map.of());

        var chats = messagingService.getChats(A, "Bearer t");
        var summary = chats.stream().filter(c -> B.equals(c.counterpartySteamId())).findFirst().orElseThrow();
        assertThat(summary.lastMessagePreview()).endsWith("...");
        assertThat(summary.lastMessagePreview().length()).isLessThanOrEqualTo(80);
    }

    @Test
    void getChats_addsSupportAndAppliesPresets() {
        var id = UUID.randomUUID();
        var msg = new Message(id, HA, HB, "ENC:" + A, "ENC:" + B, "ENC:hi", Instant.now());
        when(messageRepository.findBySenderIdOrRecipientIdOrderByCreatedAtDesc(eq(HA), eq(HA)))
                .thenReturn(List.of(msg));
        when(authClient.batchPresetAvatarIds(any(), eq("Bearer t"))).thenReturn(Map.of(B, 7));

        var chats = messagingService.getChats(A, "Bearer t");

        assertThat(chats.stream().anyMatch(c -> SupportSyntheticSteamId.RAW_VALUE.equals(c.counterpartySteamId())))
                .isTrue();
        assertThat(chats.stream().filter(c -> B.equals(c.counterpartySteamId())).findFirst())
                .hasValueSatisfying(c -> assertThat(c.counterpartyPresetAvatarId()).isEqualTo(7));
    }

    @Test
    void getChatByUsername_resolvesAndBuildsSummary() {
        when(authClient.getSteamIdByUsername(eq("bob"), any())).thenReturn(Optional.of(B));
        when(authClient.batchPresetAvatarIds(any(), any())).thenReturn(Map.of());
        when(messageRepository.findChatBetween(eq(HA), eq(HB), any())).thenReturn(List.of());

        var summary = messagingService.getChatByUsername(A, "bob", null);

        assertThat(summary.counterpartySteamId()).isEqualTo(B);
    }

    @Test
    void getChatByUsername_withLastMessage_preview() {
        when(authClient.getSteamIdByUsername(eq("bob"), any())).thenReturn(Optional.of(B));
        when(authClient.batchPresetAvatarIds(any(), any())).thenReturn(Map.of(B, 2));
        var id = UUID.randomUUID();
        var msg = new Message(id, HA, HB, "ENC:" + A, "ENC:" + B, "ENC:last", Instant.now());
        when(messageRepository.findChatBetween(eq(HA), eq(HB), any())).thenReturn(List.of(msg));

        var summary = messagingService.getChatByUsername(A, "bob", "Bearer z");

        assertThat(summary.lastMessagePreview()).isEqualTo("last");
        assertThat(summary.counterpartyPresetAvatarId()).isEqualTo(2);
    }

    @Test
    void getChatByUsername_notFound() {
        when(authClient.getSteamIdByUsername(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> messagingService.getChatByUsername(A, "nobody", null))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getChatByUsername_selfForbidden() {
        when(authClient.getSteamIdByUsername(any(), any())).thenReturn(Optional.of(A));
        assertThatThrownBy(() -> messagingService.getChatByUsername(A, "me", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteMessage_notFound() {
        when(messageRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> messagingService.deleteMessage(A, B, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteMessage_forbidden() {
        var id = UUID.randomUUID();
        var msg = new Message(id, HA, HA, "ENC:" + A, "ENC:" + A, "ENC:x", Instant.now());
        when(messageRepository.findById(id)).thenReturn(Optional.of(msg));
        assertThatThrownBy(() -> messagingService.deleteMessage(A, B, id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteMessage_tooOld() {
        var id = UUID.randomUUID();
        var old = Instant.now().minus(25, ChronoUnit.HOURS);
        var msg = new Message(id, HA, HB, "ENC:" + A, "ENC:" + B, "ENC:x", old);
        when(messageRepository.findById(id)).thenReturn(Optional.of(msg));
        assertThatThrownBy(() -> messagingService.deleteMessage(A, B, id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deleteMessage_ok() {
        var id = UUID.randomUUID();
        var msg = new Message(id, HA, HB, "ENC:" + A, "ENC:" + B, "ENC:x", Instant.now());
        when(messageRepository.findById(id)).thenReturn(Optional.of(msg));

        messagingService.deleteMessage(A, B, id);

        verify(messageRepository).delete(msg);
    }

    @Test
    void listIncomingToSupport_clampsSize() {
        when(messageRepository.findByRecipientIdOrderByCreatedAtDesc(any(), any()))
                .thenAnswer(inv -> new PageImpl<Message>(List.of(), (Pageable) inv.getArgument(1), 0));

        messagingService.listIncomingToSupport(-1, 500);

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findByRecipientIdOrderByCreatedAtDesc(any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }
}
