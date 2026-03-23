package com.upiiz.platform_api.controllers;

import com.upiiz.platform_api.dto.CancelVideoMeetingRequest;
import com.upiiz.platform_api.dto.CreateVideoMeetingRequest;
import com.upiiz.platform_api.dto.JoinVideoMeetingResponse;
import com.upiiz.platform_api.entities.VideoMeeting;
import com.upiiz.platform_api.services.VideoMeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/public/v1/video-meetings")
@RequiredArgsConstructor
public class VideoMeetingController {

    private final VideoMeetingService videoMeetingService;

    @PostMapping
    public VideoMeeting create(
            @Valid @RequestBody CreateVideoMeetingRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return videoMeetingService.create(request, currentUserId);
    }

    @GetMapping("/{id}")
    public VideoMeeting getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return videoMeetingService.getById(id, currentUserId);
    }

    @GetMapping("/mine")
    public List<VideoMeeting> getMine(
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return videoMeetingService.getMine(currentUserId);
    }

    @PostMapping("/{id}/join")
    public JoinVideoMeetingResponse join(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId,
            @RequestParam(defaultValue = "Usuario") String displayName,
            @RequestParam(required = false) String deviceInfo
    ) {
        return videoMeetingService.join(id, currentUserId, displayName, deviceInfo);
    }

    @PostMapping("/{id}/leave")
    public void leave(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        videoMeetingService.leave(id, currentUserId);
    }

    @PatchMapping("/{id}/cancel")
    public VideoMeeting cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelVideoMeetingRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return videoMeetingService.cancel(id, request, currentUserId);
    }
}