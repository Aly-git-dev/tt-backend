package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.CancelVideoMeetingRequest;
import com.upiiz.platform_api.dto.CreateVideoMeetingRequest;
import com.upiiz.platform_api.dto.JoinVideoMeetingResponse;
import com.upiiz.platform_api.entities.Appointment;
import com.upiiz.platform_api.entities.VideoMeeting;
import com.upiiz.platform_api.entities.VideoMeetingAttendance;
import com.upiiz.platform_api.models.AppointmentStatus;
import com.upiiz.platform_api.models.Modality;
import com.upiiz.platform_api.models.VideoMeetingRole;
import com.upiiz.platform_api.models.VideoMeetingStatus;
import com.upiiz.platform_api.repositories.AppointmentParticipantRepo;
import com.upiiz.platform_api.repositories.AppointmentRepo;
import com.upiiz.platform_api.repositories.VideoMeetingAttendanceRepo;
import com.upiiz.platform_api.repositories.VideoMeetingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoMeetingService {

    private static final String JITSI_DOMAIN = "meet.jit.si";

    private final AppointmentRepo appointmentRepo;
    private final AppointmentParticipantRepo appointmentParticipantRepo;
    private final VideoMeetingRepo videoMeetingRepo;
    private final VideoMeetingAttendanceRepo videoMeetingAttendanceRepo;
    private final VideoMeetingRoomNameGenerator roomNameGenerator;

    @Transactional
    public VideoMeeting create(CreateVideoMeetingRequest request, UUID currentUserId) {
        Appointment appointment = appointmentRepo.findById(request.appointmentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La cita no existe"
                ));

        if (videoMeetingRepo.existsByAppointmentId(request.appointmentId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una videoconferencia para esta cita"
            );
        }

        if (appointment.getModality() != Modality.ONLINE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se puede crear videoconferencia para citas online"
            );
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede crear videoconferencia para una cita cancelada"
            );
        }

        boolean creatorBelongs = appointmentParticipantRepo
                .existsByAppointment_IdAndUserId(request.appointmentId(), currentUserId);

        if (!creatorBelongs && !appointment.getCreatedBy().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para crear esta videoconferencia"
            );
        }

        String roomName = roomNameGenerator.generate(appointment.getTitle(), appointment.getId());
        String meetingUrl = "https://" + JITSI_DOMAIN + "/" + roomName;

        VideoMeeting vm = VideoMeeting.create(
                appointment.getId(),
                currentUserId,
                currentUserId,
                roomName,
                meetingUrl
        );

        return videoMeetingRepo.save(vm);
    }

    @Transactional(readOnly = true)
    public VideoMeeting getById(UUID meetingId, UUID currentUserId) {
        VideoMeeting vm = videoMeetingRepo.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Videoconferencia no encontrada"
                ));

        boolean belongs = appointmentParticipantRepo
                .existsByAppointment_IdAndUserId(vm.getAppointmentId(), currentUserId);

        if (!belongs && !vm.getCreatedBy().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes acceso a esta videoconferencia"
            );
        }

        return vm;
    }

    @Transactional(readOnly = true)
    public VideoMeeting getByAppointment(UUID appointmentId, UUID currentUserId) {
        VideoMeeting vm = videoMeetingRepo.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe videollamada para esta cita"
                ));

        boolean belongs = appointmentParticipantRepo
                .existsByAppointment_IdAndUserId(vm.getAppointmentId(), currentUserId);

        if (!belongs && !vm.getCreatedBy().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes acceso a esta videollamada"
            );
        }

        return vm;
    }

    @Transactional(readOnly = true)
    public List<VideoMeeting> getMine(UUID currentUserId) {
        return videoMeetingRepo.findByHostUserId(currentUserId);
    }

    @Transactional
    public JoinVideoMeetingResponse join(UUID meetingId, UUID currentUserId, String displayName, String deviceInfo) {
        VideoMeeting vm = videoMeetingRepo.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Videoconferencia no encontrada"
                ));

        boolean belongs = appointmentParticipantRepo
                .existsByAppointment_IdAndUserId(vm.getAppointmentId(), currentUserId);

        if (!belongs) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No perteneces a esta cita"
            );
        }

        if (vm.getStatus() == VideoMeetingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La videoconferencia fue cancelada"
            );
        }

        if (vm.getStatus() == VideoMeetingStatus.ENDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La videoconferencia ya terminó"
            );
        }

        vm.markLive();
        videoMeetingRepo.save(vm);

        VideoMeetingRole role = currentUserId.equals(vm.getHostUserId())
                ? VideoMeetingRole.HOST
                : VideoMeetingRole.PARTICIPANT;

        VideoMeetingAttendance attendance = VideoMeetingAttendance.create(
                vm.getId(),
                currentUserId,
                role,
                deviceInfo
        );

        videoMeetingAttendanceRepo.save(attendance);

        return new JoinVideoMeetingResponse(
                vm.getId(),
                vm.getProvider().name(),
                JITSI_DOMAIN,
                vm.getRoomName(),
                vm.getMeetingUrl(),
                displayName
        );
    }

    @Transactional
    public void leave(UUID meetingId, UUID currentUserId) {
        VideoMeeting vm = videoMeetingRepo.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Videoconferencia no encontrada"
                ));

        VideoMeetingAttendance attendance = videoMeetingAttendanceRepo
                .findTopByVideoMeetingIdAndUserIdAndLeftAtIsNullOrderByJoinedAtDesc(meetingId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No hay sesión activa para cerrar"
                ));

        attendance.closeSession();
        videoMeetingAttendanceRepo.save(attendance);

        long openSessions = videoMeetingAttendanceRepo.countByVideoMeetingIdAndLeftAtIsNull(meetingId);
        if (openSessions == 0 && vm.getStatus() == VideoMeetingStatus.LIVE) {
            vm.end();
            videoMeetingRepo.save(vm);
        }
    }

    @Transactional
    public VideoMeeting cancel(UUID meetingId, CancelVideoMeetingRequest request, UUID currentUserId) {
        VideoMeeting vm = videoMeetingRepo.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Videoconferencia no encontrada"
                ));

        if (!vm.getHostUserId().equals(currentUserId) && !vm.getCreatedBy().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para cancelar esta videoconferencia"
            );
        }

        vm.cancel(currentUserId, request.reason());
        return videoMeetingRepo.save(vm);
    }
}