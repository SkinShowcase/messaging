package com.skinsshowcase.messaging.repository;

import com.skinsshowcase.messaging.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    @Query("""
        SELECT m FROM Message m
        WHERE (m.senderId = :userSteamId AND m.recipientId = :counterpartySteamId)
           OR (m.senderId = :counterpartySteamId AND m.recipientId = :userSteamId)
        ORDER BY m.createdAt DESC
        """)
    List<Message> findChatBetween(String userSteamId, String counterpartySteamId, Pageable pageable);

    List<Message> findBySenderIdOrRecipientIdOrderByCreatedAtDesc(String senderId, String recipientId);

    @Query(value = """
        SELECT DISTINCT CASE
            WHEN sender_id = :userSteamId THEN recipient_id
            ELSE sender_id
        END AS counterparty
        FROM message
        WHERE sender_id = :userSteamId OR recipient_id = :userSteamId
        ORDER BY counterparty
        """, nativeQuery = true)
    List<String> findCounterpartySteamIdsByUser(String userSteamId);
}
