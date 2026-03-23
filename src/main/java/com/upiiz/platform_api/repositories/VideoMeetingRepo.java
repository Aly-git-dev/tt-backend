package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.VideoMeeting;
import com.upiiz.platform_api.models.VideoMeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoMeetingRepo extends JpaRepository<VideoMeeting, UUID> {

    Optional<VideoMeeting> findByAppointmentId(UUID appointmentId);

    boolean existsByAppointmentId(UUID appointmentId);

    List<VideoMeeting> findByHostUserId(UUID hostUserId);

    List<VideoMeeting> findByStatus(VideoMeetingStatus status);
}