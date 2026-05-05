package com.upiiz.platform_api.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="chat_message_report",
        indexes = {
                @Index(name="idx_chat_report_status", columnList="status"),
                @Index(name="idx_chat_report_reporter", columnList="reporter_id"),
                @Index(name="idx_chat_report_conversation", columnList="conversation_id"),
                @Index(name="idx_chat_report_message", columnList="reported_message_id")
        })
public class ChatMessageReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="reporter_id", nullable=false)
    private UUID reporterId;

    @Column(name="conversation_id", nullable=false)
    private Long conversationId;

    @Column(name="reported_message_id", nullable=false)
    private Long reportedMessageId;

    @Column(name="reason_code", nullable=false, length=30)
    private String reasonCode;

    @Column(name="description", columnDefinition="text")
    private String description;

    @Column(name="status", nullable=false, length=20)
    private String status; // PENDIENTE | EN_REVISION | RESUELTO | DESESTIMADO

    @Column(name="handled_by")
    private UUID handledBy;

    @Column(name="handled_at")
    private Instant handledAt;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    // getters/setters
    public Long getId() { return id; }
    public UUID getReporterId() { return reporterId; }
    public void setReporterId(UUID reporterId) { this.reporterId = reporterId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getReportedMessageId() { return reportedMessageId; }
    public void setReportedMessageId(Long reportedMessageId) { this.reportedMessageId = reportedMessageId; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getHandledBy() { return handledBy; }
    public void setHandledBy(UUID handledBy) { this.handledBy = handledBy; }
    public Instant getHandledAt() { return handledAt; }
    public void setHandledAt(Instant handledAt) { this.handledAt = handledAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

