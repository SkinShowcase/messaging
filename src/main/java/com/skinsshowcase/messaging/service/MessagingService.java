package com.skinsshowcase.messaging.service;

import com.skinsshowcase.messaging.client.AuthClient;
import com.skinsshowcase.messaging.config.SupportSyntheticSteamId;
import com.skinsshowcase.messaging.dto.AdminSupportInboxPageDto;
import com.skinsshowcase.messaging.dto.ChatSummaryDto;
import com.skinsshowcase.messaging.dto.MessageResponseDto;
import com.skinsshowcase.messaging.dto.SendMessageRequestDto;
import com.skinsshowcase.messaging.entity.Message;
import com.skinsshowcase.messaging.exception.UserNotFoundException;
import com.skinsshowcase.messaging.metrics.MessagingMetrics;
import com.skinsshowcase.messaging.repository.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MessagingService {

    private static final Pattern STEAM_ID_PATTERN = Pattern.compile("^[0-9]{17}$");
    private static final Pattern HASH_64_HEX_PATTERN = Pattern.compile("^[a-f0-9]{64}$");

    private final MessageRepository messageRepository;
    private final MessageNotificationService messageNotificationService;
    private final AuthClient authClient;
    private final MessagingMetrics messagingMetrics;
    private final MessageCryptoService messageCryptoService;
    private final SteamIdHashingService steamIdHashingService;

    public MessagingService(MessageRepository messageRepository,
                            MessageNotificationService messageNotificationService,
                            AuthClient authClient,
                            MessagingMetrics messagingMetrics,
                            MessageCryptoService messageCryptoService,
                            SteamIdHashingService steamIdHashingService) {
        this.messageRepository = messageRepository;
        this.messageNotificationService = messageNotificationService;
        this.authClient = authClient;
        this.messagingMetrics = messagingMetrics;
        this.messageCryptoService = messageCryptoService;
        this.steamIdHashingService = steamIdHashingService;
    }

    /**
     * Сообщение от поддержки пользователю (отправитель — синтетический {@link SupportSyntheticSteamId#VALUE}).
     */
    @Transactional
    public MessageResponseDto sendSupportMessageToUser(String recipientSteamId, String textRaw) {
        validateSteamId(recipientSteamId);
        var dto = new SendMessageRequestDto(textRaw.trim());
        var result = sendMessage(SupportSyntheticSteamId.RAW_VALUE, recipientSteamId, dto);
        messagingMetrics.recordMessageSent("support_to_user");
        return result;
    }

    /**
     * Сообщения пользователей синтетической поддержке (получатель — {@link SupportSyntheticSteamId#VALUE}).
     */
    @Transactional(readOnly = true)
    public AdminSupportInboxPageDto listIncomingToSupport(int page, int rawSize) {
        var size = Math.clamp(rawSize, 1, 100);
        var safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, size);
        var result = messageRepository.findByRecipientIdOrderByCreatedAtDesc(
                SupportSyntheticSteamId.VALUE, pageable);
        var content = mapMessagesToDto(result.getContent());
        return new AdminSupportInboxPageDto(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private List<MessageResponseDto> mapMessagesToDto(List<Message> messages) {
        var list = new ArrayList<MessageResponseDto>();
        for (var m : messages) {
            list.add(toDtoHashedIds(m));
        }
        return list;
    }

    @Transactional
    public MessageResponseDto sendMessage(String senderSteamId, String recipientSteamId, SendMessageRequestDto dto) {
        validateSteamId(recipientSteamId);
        if (senderSteamId.equals(recipientSteamId)) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        var id = UUID.randomUUID();
        var now = Instant.now();
        var plainText = dto.text().trim();
        var encryptedText = messageCryptoService.encrypt(plainText);
        var senderHash = toStoredSteamId(senderSteamId);
        var recipientHash = toStoredSteamId(recipientSteamId);
        var senderEnc = messageCryptoService.encrypt(normalizePublicSteamId(senderSteamId));
        var recipientEnc = messageCryptoService.encrypt(normalizePublicSteamId(recipientSteamId));
        var message = new Message(id, senderHash, recipientHash, senderEnc, recipientEnc, encryptedText, now);
        messageRepository.save(message);

        messageNotificationService.notifyNewMessage(senderSteamId, recipientSteamId, id, plainText);

        if (!SupportSyntheticSteamId.HASH_VALUE.equals(senderHash)) {
            messagingMetrics.recordMessageSent("user_to_user");
        }

        return new MessageResponseDto(id, senderSteamId, recipientSteamId, plainText, now);
    }

    public List<MessageResponseDto> getChatHistory(String userSteamId, String counterpartySteamId, int page, int size) {
        validateSteamId(counterpartySteamId);
        messagingMetrics.recordChatHistoryRequest();
        var pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        var userHash = toStoredSteamId(userSteamId);
        var counterpartyHash = toStoredSteamId(counterpartySteamId);
        var messages = messageRepository.findChatBetween(userHash, counterpartyHash, pageable);
        return messages.stream().map(m -> toDtoForChat(m, userSteamId, counterpartySteamId, userHash)).toList();
    }

    public List<ChatSummaryDto> getChats(String userSteamId, String authorizationHeader) {
        messagingMetrics.recordChatListRequest();
        var userStoredId = toStoredSteamId(userSteamId);
        var latestByCounterparty = latestMessagesByCounterparty(userStoredId);
        var counterpartyIds = new ArrayList<>(latestByCounterparty.keySet());
        var presetBySteamId = authClient.batchPresetAvatarIds(counterpartyIds, authorizationHeader);
        var list = new ArrayList<ChatSummaryDto>();
        for (var counterparty : counterpartyIds) {
            var lastMessage = latestByCounterparty.get(counterparty);
            list.add(buildChatSummary(counterparty, lastMessage, presetBySteamId));
        }
        ensureSupportChatInList(list, presetBySteamId);
        return list;
    }

    /**
     * Найти чат по имени пользователя (Steam ID или persona name). Резолв через auth.
     */
    public ChatSummaryDto getChatByUsername(String userSteamId, String username, String authorizationHeader) {
        var counterpartySteamId = authClient.getSteamIdByUsername(username, authorizationHeader)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        if (counterpartySteamId.equals(userSteamId)) {
            throw new IllegalArgumentException("Cannot get chat with yourself");
        }
        var presetBySteamId = authClient.batchPresetAvatarIds(List.of(counterpartySteamId), authorizationHeader);
        var userStored = toStoredSteamId(userSteamId);
        var counterpartyStored = toStoredSteamId(counterpartySteamId);
        var lastMessages = messageRepository.findChatBetween(userStored, counterpartyStored, PageRequest.of(0, 1));
        var lastMessage = lastMessages.isEmpty() ? null : lastMessages.get(0);
        return buildChatSummary(counterpartySteamId, lastMessage, presetBySteamId);
    }

    @Transactional
    public void deleteMessage(String userSteamId, String counterpartySteamId, UUID messageId) {
        validateSteamId(counterpartySteamId);
        var msg = messageRepository.findById(messageId).orElse(null);
        if (msg == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
        }
        if (!isChatParticipant(msg, toStoredSteamId(userSteamId), toStoredSteamId(counterpartySteamId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a chat participant");
        }
        if (!isYoungerThanTwentyFourHours(msg)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only messages newer than 24 hours can be deleted");
        }
        messageRepository.delete(msg);
    }

    private void ensureSupportChatInList(List<ChatSummaryDto> list, Map<String, Integer> presetBySteamId) {
        var supportSteamId = SupportSyntheticSteamId.RAW_VALUE;
        var alreadyHasSupport = list.stream()
                .anyMatch(c -> supportSteamId.equals(c.counterpartySteamId()));
        if (!alreadyHasSupport) {
            list.add(0, buildChatSummary(supportSteamId, null, presetBySteamId));
        }
    }

    private ChatSummaryDto buildChatSummary(String counterpartySteamId, Message lastMessage, Map<String, Integer> presetBySteamId) {
        var support = isSupportCounterparty(counterpartySteamId);
        var presetId = lookupPresetId(presetBySteamId, counterpartySteamId);
        if (lastMessage == null) {
            return new ChatSummaryDto(counterpartySteamId, "", Instant.EPOCH, support, presetId);
        }
        var preview = truncatePreview(messageCryptoService.decrypt(lastMessage.getText()));
        return new ChatSummaryDto(counterpartySteamId, preview, lastMessage.getCreatedAt(), support, presetId);
    }

    private static Integer lookupPresetId(Map<String, Integer> presetBySteamId, String counterpartySteamId) {
        if (presetBySteamId == null || presetBySteamId.isEmpty()) {
            return null;
        }
        return presetBySteamId.get(counterpartySteamId);
    }

    private static boolean isSupportCounterparty(String counterpartySteamId) {
        if (counterpartySteamId == null) {
            return false;
        }
        return SupportSyntheticSteamId.RAW_VALUE.equals(counterpartySteamId.trim());
    }

    private static String truncatePreview(String text) {
        if (text == null || text.length() <= 80) {
            return text == null ? "" : text;
        }
        return text.substring(0, 77) + "...";
    }

    private MessageResponseDto toDtoForChat(Message m, String userSteamId, String counterpartySteamId, String userHash) {
        var senderSteamId = m.getSenderId().equals(userHash) ? userSteamId : counterpartySteamId;
        var recipientSteamId = m.getSenderId().equals(userHash) ? counterpartySteamId : userSteamId;
        return new MessageResponseDto(
                m.getId(),
                senderSteamId,
                recipientSteamId,
                messageCryptoService.decrypt(m.getText()),
                m.getCreatedAt()
        );
    }

    private MessageResponseDto toDtoHashedIds(Message m) {
        var senderSteamId = resolvePublicSteamId(m.getSenderId(), m.getSenderIdEnc());
        var recipientSteamId = resolvePublicSteamId(m.getRecipientId(), m.getRecipientIdEnc());
        return new MessageResponseDto(
                m.getId(),
                senderSteamId,
                recipientSteamId,
                messageCryptoService.decrypt(m.getText()),
                m.getCreatedAt()
        );
    }

    private static void validateSteamId(String steamId) {
        if (steamId == null || !STEAM_ID_PATTERN.matcher(steamId).matches()) {
            throw new IllegalArgumentException("Invalid Steam ID: must be 17 digits");
        }
    }

    private static boolean isChatParticipant(Message msg, String userSteamId, String counterpartySteamId) {
        var forward = msg.getSenderId().equals(userSteamId) && msg.getRecipientId().equals(counterpartySteamId);
        var backward = msg.getSenderId().equals(counterpartySteamId) && msg.getRecipientId().equals(userSteamId);
        return forward || backward;
    }

    private static boolean isYoungerThanTwentyFourHours(Message msg) {
        var cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        return msg.getCreatedAt().isAfter(cutoff);
    }

    private String toStoredSteamId(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            return steamId;
        }
        var trimmed = steamId.trim();
        if (SupportSyntheticSteamId.RAW_VALUE.equals(trimmed) || SupportSyntheticSteamId.HASH_VALUE.equals(trimmed)) {
            return SupportSyntheticSteamId.HASH_VALUE;
        }
        if (HASH_64_HEX_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }
        return steamIdHashingService.sha256(trimmed);
    }

    private String resolvePublicSteamId(String storedSteamId, String encryptedSteamId) {
        if (storedSteamId == null) {
            return null;
        }
        var trimmed = storedSteamId.trim();
        if (SupportSyntheticSteamId.HASH_VALUE.equals(trimmed)) {
            return SupportSyntheticSteamId.RAW_VALUE;
        }
        if (trimmed.matches("^[0-9]{17}$")) {
            return trimmed;
        }
        if (encryptedSteamId != null && !encryptedSteamId.isBlank()) {
            var decrypted = messageCryptoService.decrypt(encryptedSteamId);
            if (decrypted != null && STEAM_ID_PATTERN.matcher(decrypted).matches()) {
                return decrypted;
            }
        }
        return trimmed;
    }

    private Map<String, Message> latestMessagesByCounterparty(String userStoredId) {
        var all = messageRepository.findBySenderIdOrRecipientIdOrderByCreatedAtDesc(userStoredId, userStoredId);
        var out = new LinkedHashMap<String, Message>();
        for (var m : all) {
            var userIsSender = userStoredId.equals(m.getSenderId());
            var counterpartyStored = userIsSender ? m.getRecipientId() : m.getSenderId();
            var counterpartyEnc = userIsSender ? m.getRecipientIdEnc() : m.getSenderIdEnc();
            var counterpartyPublic = resolvePublicSteamId(counterpartyStored, counterpartyEnc);
            if (counterpartyPublic == null || counterpartyPublic.isBlank()) {
                continue;
            }
            out.putIfAbsent(counterpartyPublic, m);
        }
        return out;
    }

    private static String normalizePublicSteamId(String steamId) {
        if (steamId == null) {
            return null;
        }
        var trimmed = steamId.trim();
        if (SupportSyntheticSteamId.HASH_VALUE.equals(trimmed)) {
            return SupportSyntheticSteamId.RAW_VALUE;
        }
        return trimmed;
    }
}
