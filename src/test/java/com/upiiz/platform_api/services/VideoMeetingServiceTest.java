package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.JoinVideoMeetingResponse;
import com.upiiz.platform_api.entities.Appointment;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.entities.VideoMeeting;
import com.upiiz.platform_api.entities.VideoMeetingAttendance;
import com.upiiz.platform_api.models.AppointmentStatus;
import com.upiiz.platform_api.models.Modality;
import com.upiiz.platform_api.models.ParticipantRole;
import com.upiiz.platform_api.models.RSVPStatus;
import com.upiiz.platform_api.models.VideoMeetingRole;
import com.upiiz.platform_api.models.VideoMeetingStatus;
import com.upiiz.platform_api.repositories.AppointmentParticipantRepo;
import com.upiiz.platform_api.repositories.AppointmentRepo;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.repositories.VideoMeetingAttendanceRepo;
import com.upiiz.platform_api.repositories.VideoMeetingRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoMeetingServiceTest {

    @Mock
    private AppointmentRepo appointmentRepo;

    @Mock
    private AppointmentParticipantRepo appointmentParticipantRepo;

    @Mock
    private VideoMeetingRepo videoMeetingRepo;

    @Mock
    private VideoMeetingAttendanceRepo videoMeetingAttendanceRepo;

    @Mock
    private VideoMeetingRoomNameGenerator roomNameGenerator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JaasJwtService jaasJwtService;

    @Test
    void joinRejectsEmbeddedDemoFallbackWhenJaasIsDisabled() {
        UUID hostId = UUID.randomUUID();
        Appointment appointment = onlineAppointment(hostId);
        VideoMeeting meeting = VideoMeeting.create(
                appointment.getId(),
                hostId,
                hostId,
                "upiiz-room",
                "https://meet.jit.si/upiiz-room"
        );

        when(videoMeetingRepo.findById(meeting.getId())).thenReturn(Optional.of(meeting));
        when(appointmentRepo.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentParticipantRepo.existsByAppointment_IdAndUserId(appointment.getId(), hostId))
                .thenReturn(true);

        VideoMeetingService service = service();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.join(meeting.getId(), hostId, "Chrome")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals(VideoMeetingStatus.SCHEDULED, meeting.getStatus());
        verify(videoMeetingRepo, never()).save(any(VideoMeeting.class));
    }

    @Test
    void joinReturnsJaasRoomAndJwtWhenJaasIsEnabled() {
        UUID hostId = UUID.randomUUID();
        Appointment appointment = onlineAppointment(hostId);
        VideoMeeting meeting = VideoMeeting.create(
                appointment.getId(),
                hostId,
                hostId,
                "upiiz-room",
                "https://8x8.vc/vpaas-magic-cookie-test/upiiz-room"
        );
        User host = User.builder()
                .id(hostId)
                .nombre("Ana Lopez")
                .avatarUrl("/profiles/ana.png")
                .build();

        when(videoMeetingRepo.findById(meeting.getId())).thenReturn(Optional.of(meeting));
        when(appointmentRepo.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentParticipantRepo.existsByAppointment_IdAndUserId(appointment.getId(), hostId))
                .thenReturn(true);
        when(videoMeetingRepo.save(any(VideoMeeting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(videoMeetingAttendanceRepo.save(any(VideoMeetingAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jaasJwtService.isEnabled()).thenReturn(true);
        when(jaasJwtService.domain()).thenReturn("8x8.vc");
        when(jaasJwtService.appId()).thenReturn("vpaas-magic-cookie-test");
        when(jaasJwtService.fullRoomName("upiiz-room")).thenReturn("vpaas-magic-cookie-test/upiiz-room");
        when(jaasJwtService.meetingUrl("upiiz-room")).thenReturn("https://8x8.vc/vpaas-magic-cookie-test/upiiz-room");
        when(jaasJwtService.externalApiUrl()).thenReturn("https://8x8.vc/vpaas-magic-cookie-test/external_api.js");
        when(jaasJwtService.createToken(host, true, "upiiz-room")).thenReturn("jaas.jwt");

        VideoMeetingService service = service();

        JoinVideoMeetingResponse response = service.join(meeting.getId(), hostId, "Chrome");

        assertEquals("8x8.vc", response.domain());
        assertEquals("vpaas-magic-cookie-test/upiiz-room", response.roomName());
        assertEquals("https://8x8.vc/vpaas-magic-cookie-test/upiiz-room", response.meetingUrl());
        assertEquals("Ana Lopez", response.displayName());
        assertEquals("/profiles/ana.png", response.avatarUrl());
        assertEquals(hostId, response.userId());
        assertEquals(VideoMeetingRole.HOST.name(), response.role());
        assertEquals("/video-meetings/" + meeting.getId() + "/room", response.appPath());
        assertEquals("https://8x8.vc/vpaas-magic-cookie-test/external_api.js", response.externalApiUrl());
        assertEquals("vpaas-magic-cookie-test", response.appId());
        assertEquals("jaas.jwt", response.jwt());
        assertTrue(response.host());
    }

    @Test
    void hostEndClosesOpenSessionsAndCompletesAppointment() {
        UUID hostId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Appointment appointment = onlineAppointment(hostId);
        appointment.addParticipant(participantId, ParticipantRole.ATTENDEE, RSVPStatus.ACCEPTED);
        VideoMeeting meeting = VideoMeeting.create(
                appointment.getId(),
                hostId,
                hostId,
                "upiiz-room",
                "https://8x8.vc/vpaas-magic-cookie-test/upiiz-room"
        );
        meeting.markLive();

        VideoMeetingAttendance hostAttendance = VideoMeetingAttendance.create(
                meeting.getId(),
                hostId,
                VideoMeetingRole.HOST,
                "Chrome"
        );
        VideoMeetingAttendance participantAttendance = VideoMeetingAttendance.create(
                meeting.getId(),
                participantId,
                VideoMeetingRole.PARTICIPANT,
                "Firefox"
        );

        when(videoMeetingRepo.findById(meeting.getId())).thenReturn(Optional.of(meeting));
        when(videoMeetingAttendanceRepo.findByVideoMeetingIdAndLeftAtIsNull(meeting.getId()))
                .thenReturn(List.of(hostAttendance, participantAttendance));
        when(videoMeetingRepo.save(any(VideoMeeting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepo.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoMeetingService service = service();

        VideoMeeting ended = service.end(meeting.getId(), hostId);

        assertEquals(VideoMeetingStatus.ENDED, ended.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
        assertNotNull(hostAttendance.getLeftAt());
        assertNotNull(participantAttendance.getLeftAt());

        ArgumentCaptor<List<VideoMeetingAttendance>> attendanceCaptor = ArgumentCaptor.forClass(List.class);
        verify(videoMeetingAttendanceRepo).saveAll(attendanceCaptor.capture());
        assertEquals(2, attendanceCaptor.getValue().size());
    }

    private VideoMeetingService service() {
        return new VideoMeetingService(
                appointmentRepo,
                appointmentParticipantRepo,
                videoMeetingRepo,
                videoMeetingAttendanceRepo,
                roomNameGenerator,
                userRepository,
                jaasJwtService
        );
    }

    private Appointment onlineAppointment(UUID hostId) {
        LocalDateTime startsAt = LocalDateTime.now().plusHours(1);
        Appointment appointment = Appointment.create(
                hostId,
                "Asesoria",
                "Revision",
                Modality.ONLINE,
                startsAt,
                startsAt.plusHours(1)
        );
        appointment.addParticipant(hostId, ParticipantRole.HOST, RSVPStatus.ACCEPTED);
        return appointment;
    }
}
