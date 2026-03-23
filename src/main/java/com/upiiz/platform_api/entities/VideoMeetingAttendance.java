package com.upiiz.platform_api.entities;

import com.upiiz.platform_api.models.VideoMeetingRole;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_meeting_attendance")
public class VideoMeetingAttendance {

    @Id
    private UUID id;

    @Column(name = "video_meeting_id", nullable = false)
    private UUID videoMeetingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_meeting", nullable = false, length = 20)
    private VideoMeetingRole roleInMeeting;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "session_seconds")
    private Integer sessionSeconds;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected VideoMeetingAttendance() {}

    public static VideoMeetingAttendance create(
            UUID videoMeetingId,
            UUID userId,
            VideoMeetingRole roleInMeeting,
            String deviceInfo
    ) {
        VideoMeetingAttendance a = new VideoMeetingAttendance();
        a.id = UUID.randomUUID();
        a.videoMeetingId = videoMeetingId;
        a.userId = userId;
        a.roleInMeeting = roleInMeeting;
        a.deviceInfo = deviceInfo;
        a.joinedAt = LocalDateTime.now();
        a.createdAt = a.joinedAt;
        return a;
    }

    public void closeSession() {
        if (this.leftAt == null) {
            this.leftAt = LocalDateTime.now();
            this.sessionSeconds = (int) Duration.between(this.joinedAt, this.leftAt).getSeconds();
        }
    }

    public UUID getId() { return id; }
    public UUID getVideoMeetingId() { return videoMeetingId; }
    public UUID getUserId() { return userId; }
    public VideoMeetingRole getRoleInMeeting() { return roleInMeeting; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getLeftAt() { return leftAt; }
    public Integer getSessionSeconds() { return sessionSeconds; }
    public String getDeviceInfo() { return deviceInfo; }
}