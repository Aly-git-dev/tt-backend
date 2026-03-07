package com.upiiz.platform_api.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="chat_sanction",
        indexes = {
                @Index(name="idx_chat_sanction_target", columnList="target_user_id"),
                @Index(name="idx_chat_sanction_admin", columnList="admin_id"),
                @Index(name="idx_chat_sanction_type", columnList="type"),
                @Index(name="idx_chat_sanction_report", columnList="report_id")
        })
public class ChatSanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="report_id")
    private Long reportId;

    @Column(name="target_user_id", nullable=false)
    private UUID targetUserId;

    @Column(name="admin_id", nullable=false)
    private UUID adminId;

    @Column(name="type", nullable=false, length=20)
    private String type; // WARNING | TEMP_BLOCK | BAN | MUTE

    @Column(name="notes", columnDefinition="text")
    private String notes;

    @Column(name="start_at", nullable=false)
    private Instant startAt;

    @Column(name="end_at")
    private Instant endAt;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    // getters/setters
    public Long getId() { return id; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public UUID getTargetUserId() { return targetUserId; }
    public void setTargetUserId(UUID targetUserId) { this.targetUserId = targetUserId; }
    public UUID getAdminId() { return adminId; }
    public void setAdminId(UUID adminId) { this.adminId = adminId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
}

