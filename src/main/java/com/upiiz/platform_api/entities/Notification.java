package com.upiiz.platform_api.entities;

import com.upiiz.platform_api.models.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(name="user_id", nullable=false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=40)
    private NotificationType type;

    @Column(nullable=false, length=140)
    private String title;

    @Column(columnDefinition="text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name="target_type", length=20)
    private TargetType targetType;

    @Column(name="target_id")
    private UUID targetId;

    @Column(name="read_at")
    private LocalDateTime readAt;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    protected Notification(){}

    public static Notification of(UUID userId, NotificationType type, String title, String body,
                                  TargetType targetType, UUID targetId) {
        Notification n = new Notification();
        n.id = UUID.randomUUID();
        n.userId = userId;
        n.type = type;
        n.title = title;
        n.body = body;
        n.targetType = targetType;
        n.targetId = targetId;
        n.createdAt = LocalDateTime.now();
        return n;
    }
}
