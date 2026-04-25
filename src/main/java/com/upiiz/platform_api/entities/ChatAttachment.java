package com.upiiz.platform_api.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="chat_attachment",
        indexes = {
                @Index(name="idx_chat_attachment_message", columnList="message_id"),
                @Index(name="idx_chat_attachment_mime", columnList="mime_type")
        })
public class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="message_id", nullable=false)
    private Long messageId;

    @Column(name="original_name", nullable=false, length=255)
    private String originalName;

    @Column(name="mime_type", nullable=false, length=120)
    private String mimeType;

    @Column(name="size_bytes", nullable=false)
    private long sizeBytes;

    @Column(name="storage_path", nullable=false, columnDefinition="text")
    private String storagePath;

    @Column(name="checksum", length=128)
    private String checksum;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // getters/setters
    public Long getId() { return id; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public Instant getCreatedAt() { return createdAt; }
}