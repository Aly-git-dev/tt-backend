package com.upiiz.platform_api.entities;

import com.upiiz.platform_api.models.VideoMeetingProvider;
import com.upiiz.platform_api.models.VideoMeetingStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_meetings")
public class VideoMeeting {

    @Id
    private UUID id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoMeetingProvider provider;

    @Column(name = "room_name", nullable = false, unique = true, length = 180)
    private String roomName;

    @Column(name = "meeting_url", nullable = false, columnDefinition = "text")
    private String meetingUrl;

    @Column(name = "host_user_id", nullable = false)
    private UUID hostUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoMeetingStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected VideoMeeting() {}

    public static VideoMeeting create(
            UUID appointmentId,
            UUID hostUserId,
            UUID createdBy,
            String roomName,
            String meetingUrl
    ) {
        VideoMeeting vm = new VideoMeeting();
        vm.id = UUID.randomUUID();
        vm.appointmentId = appointmentId;
        vm.provider = VideoMeetingProvider.JITSI;
        vm.roomName = roomName;
        vm.meetingUrl = meetingUrl;
        vm.hostUserId = hostUserId;
        vm.status = VideoMeetingStatus.SCHEDULED;
        vm.createdBy = createdBy;
        vm.createdAt = LocalDateTime.now();
        vm.updatedAt = vm.createdAt;
        return vm;
    }

    public void markLive() {
        if (this.status == VideoMeetingStatus.SCHEDULED) {
            this.status = VideoMeetingStatus.LIVE;
            this.startedAt = LocalDateTime.now();
            this.updatedAt = this.startedAt;
        }
    }

    public void cancel(UUID cancelledBy, String reason) {
        this.status = VideoMeetingStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = this.cancelledAt;
    }

    public void end() {
        this.status = VideoMeetingStatus.ENDED;
        this.endedAt = LocalDateTime.now();
        this.updatedAt = this.endedAt;
    }

    public UUID getId() { return id; }
    public UUID getAppointmentId() { return appointmentId; }
    public VideoMeetingProvider getProvider() { return provider; }
    public String getRoomName() { return roomName; }
    public String getMeetingUrl() { return meetingUrl; }
    public UUID getHostUserId() { return hostUserId; }
    public VideoMeetingStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getCancelledBy() { return cancelledBy; }
    public String getCancelReason() { return cancelReason; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}