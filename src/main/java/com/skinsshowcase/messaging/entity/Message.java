package com.skinsshowcase.messaging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sender_id", nullable = false, length = 64)
    private String senderId;

    @Column(name = "recipient_id", nullable = false, length = 64)
    private String recipientId;

    @Column(name = "sender_id_enc", columnDefinition = "TEXT")
    private String senderIdEnc;

    @Column(name = "recipient_id_enc", columnDefinition = "TEXT")
    private String recipientIdEnc;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Message() {
    }

    public Message(UUID id, String senderId, String recipientId, String senderIdEnc, String recipientIdEnc, String text, Instant createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.senderIdEnc = senderIdEnc;
        this.recipientIdEnc = recipientIdEnc;
        this.text = text;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getText() {
        return text;
    }

    public String getSenderIdEnc() {
        return senderIdEnc;
    }

    public String getRecipientIdEnc() {
        return recipientIdEnc;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
