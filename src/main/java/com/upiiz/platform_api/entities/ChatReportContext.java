package com.upiiz.platform_api.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name="chat_report_context",
        indexes = { @Index(name="idx_chat_report_context_report", columnList="report_id") })
@IdClass(ChatReportContext.PK.class)
public class ChatReportContext {

    @Id
    @Column(name="report_id", nullable=false)
    private Long reportId;

    @Id
    @Column(name="context_index", nullable=false)
    private short contextIndex; // 0..5

    @Column(name="message_id")
    private Long messageId;

    @Column(name="sender_id_snapshot")
    private UUID senderIdSnapshot;

    @Column(name="sender_role_snapshot", length=30)
    private String senderRoleSnapshot;

    @Column(name="content_snapshot", columnDefinition="text")
    private String contentSnapshot;

    @Column(name="content_type_snapshot", length=20)
    private String contentTypeSnapshot;

    @Column(name="created_at_snapshot")
    private Instant createdAtSnapshot;

    public static class PK implements Serializable {
        private Long reportId;
        private short contextIndex;
        public PK() {}
        public PK(Long reportId, short contextIndex) { this.reportId = reportId; this.contextIndex = contextIndex; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return contextIndex == pk.contextIndex && Objects.equals(reportId, pk.reportId);
        }
        @Override public int hashCode() { return Objects.hash(reportId, contextIndex); }
    }

    // getters/setters
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public short getContextIndex() { return contextIndex; }
    public void setContextIndex(short contextIndex) { this.contextIndex = contextIndex; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public UUID getSenderIdSnapshot() { return senderIdSnapshot; }
    public void setSenderIdSnapshot(UUID senderIdSnapshot) { this.senderIdSnapshot = senderIdSnapshot; }
    public String getSenderRoleSnapshot() { return senderRoleSnapshot; }
    public void setSenderRoleSnapshot(String senderRoleSnapshot) { this.senderRoleSnapshot = senderRoleSnapshot; }
    public String getContentSnapshot() { return contentSnapshot; }
    public void setContentSnapshot(String contentSnapshot) { this.contentSnapshot = contentSnapshot; }
    public String getContentTypeSnapshot() { return contentTypeSnapshot; }
    public void setContentTypeSnapshot(String contentTypeSnapshot) { this.contentTypeSnapshot = contentTypeSnapshot; }
    public Instant getCreatedAtSnapshot() { return createdAtSnapshot; }
    public void setCreatedAtSnapshot(Instant createdAtSnapshot) { this.createdAtSnapshot = createdAtSnapshot; }
}

