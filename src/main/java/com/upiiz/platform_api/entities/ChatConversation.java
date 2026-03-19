package com.upiiz.platform_api.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_conversation",
        indexes = {
                @Index(name = "idx_chat_conversation_user1", columnList = "user1_id"),
                @Index(name = "idx_chat_conversation_user2", columnList = "user2_id"),
                @Index(name = "idx_chat_conversation_lastmsg", columnList = "last_message_at")
        })
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @Column(name = "allowed_pair", nullable = false, length = 30)
    private String allowedPair;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public UUID getUser1Id() { return user1Id; }
    public void setUser1Id(UUID user1Id) { this.user1Id = user1Id; }

    public UUID getUser2Id() { return user2Id; }
    public void setUser2Id(UUID user2Id) { this.user2Id = user2Id; }

    public String getAllowedPair() { return allowedPair; }
    public void setAllowedPair(String allowedPair) { this.allowedPair = allowedPair; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}