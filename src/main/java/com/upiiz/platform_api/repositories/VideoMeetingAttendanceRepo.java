package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.VideoMeetingAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoMeetingAttendanceRepo extends JpaRepository<VideoMeetingAttendance, UUID> {

    List<VideoMeetingAttendance> findByVideoMeetingId(UUID videoMeetingId);

    Optional<VideoMeetingAttendance> findTopByVideoMeetingIdAndUserIdAndLeftAtIsNullOrderByJoinedAtDesc(
            UUID videoMeetingId,
            UUID userId
    );

    long countByVideoMeetingIdAndLeftAtIsNull(UUID videoMeetingId);
}
