package com.upiiz.platform_api.entities;

import com.upiiz.platform_api.models.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="reminders")
public class Reminder {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name="target_type", nullable=false, length=20)
    private TargetType targetType;

    @Column(name="target_id", nullable=false)
    private UUID targetId;

    @Column(name="user_id", nullable=false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private ReminderChannel channel;

    @Column(name="remind_at", nullable=false)
    private LocalDateTime remindAt;

    @Column(name="sent_at")
    private LocalDateTime sentAt;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    protected Reminder() {}

    public static Reminder create(TargetType targetType, UUID targetId, UUID userId, ReminderChannel channel, LocalDateTime remindAt) {
        Reminder r = new Reminder();
        r.id = UUID.randomUUID();
        r.targetType = targetType;
        r.targetId = targetId;
        r.userId = userId;
        r.channel = channel;
        r.remindAt = remindAt;
        r.createdAt = LocalDateTime.now();
        return r;
    }

    public void markSent() { this.sentAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public ReminderChannel getChannel() { return channel; }
    public UUID getTargetId() { return targetId; }
}
