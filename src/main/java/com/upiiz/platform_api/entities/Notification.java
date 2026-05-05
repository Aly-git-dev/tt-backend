package com.upiiz.platform_api.entities;

import com.upiiz.platform_api.models.NotificationType;
import com.upiiz.platform_api.models.TargetType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setUserId(UUID userId) { this.userId = userId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setBody(String body) { this.body = body; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public boolean isRead() {
        return readAt != null;
    }

    public static Notification of(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            TargetType targetType,
            UUID targetId
    ) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type.name());          // 👈 importante si guardas como VARCHAR
        n.setTitle(title);
        n.setBody(body);
        n.setTargetType(targetType.name());
        n.setTargetId(
                targetId != null ? String.valueOf(UUID.fromString(targetId.toString())) : null
        );
        return n;
    }
}
