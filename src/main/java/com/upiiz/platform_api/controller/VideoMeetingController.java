package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.CancelVideoMeetingRequest;
import com.upiiz.platform_api.dto.CreateVideoMeetingRequest;
import com.upiiz.platform_api.dto.JoinVideoMeetingResponse;
import com.upiiz.platform_api.dto.VideoMeetingParticipantResponse;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.entities.VideoMeeting;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.services.VideoMeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/public/v1/video-meetings")
@RequiredArgsConstructor
@Tag(name = "Videoconferencias", description = "Creacion, consulta, entrada, salida y cancelacion de videoconferencias")
@SecurityRequirement(name = "bearer-jwt")
public class VideoMeetingController {

    private final VideoMeetingService videoMeetingService;
    private final UserRepository userRepository;

    private UUID getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmailInst(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("Usuario autenticado no encontrado con email: " + email)
                );

        return user.getId();
    }

    @PostMapping
    @Operation(summary = "Crear videoconferencia", description = "Crea una sala Jitsi asociada a una cita online.")
    public VideoMeeting create(
            @Valid @RequestBody CreateVideoMeetingRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = getCurrentUserId(authentication);
        return videoMeetingService.create(request, currentUserId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener videoconferencia", description = "Devuelve una videoconferencia si el usuario pertenece a la cita.")
    public VideoMeeting getById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = getCurrentUserId(authentication);
        return videoMeetingService.getById(id, currentUserId);
    }

    @GetMapping("/by-appointment/{appointmentId}")
    @Operation(summary = "Obtener por cita", description = "Devuelve la videoconferencia asociada a una cita.")
    public VideoMeeting getByAppointment(
            @PathVariable UUID appointmentId,
            Authentication authentication
    ) {
        UUID currentUserId = getCurrentUserId(authentication);
        return videoMeetingService.getByAppointment(appointmentId, currentUserId);
    }

    @GetMapping("/mine")
    @Operation(summary = "Listar mis videoconferencias", description = "Lista videoconferencias donde el usuario autenticado es anfitrion.")
    public List<VideoMeeting> getMine(Authentication authentication) {
        UUID currentUserId = getCurrentUserId(authentication);
        return videoMeetingService.getMine(currentUserId);
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Entrar a videoconferencia", description = "Registra asistencia y devuelve los datos necesarios para embeber Jitsi dentro de la app.")
    public JoinVideoMeetingResponse join(
            @PathVariable UUID id,
            @RequestParam(required = false) String deviceInfo,
            Authentication authentication
    ) {
        UUID currentUserId = getCurrentUserId(authentication);
        return videoMeetingService.join(id, currentUserId, deviceInfo);
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Listar participantes activos", description = "Devuelve nombre, foto de perfil y rol de los usuarios actualmente dentro de la videollamada.")
    public List<VideoMeetingParticipantResponse> participants(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = getCurrentUserId(authentication);
        return videoMeetingService.getActiveParticipants(id, currentUserId);
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Salir de videoconferencia", description = "Cierra la sesion activa de asistencia del usuario.")
    public void leave(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = getCurrentUserId(authentication);
        videoMeetingService.leave(id, currentUserId);
    }

    @PostMapping("/{id}/end")
    @Operation(summary = "Finalizar videoconferencia", description = "Finaliza la videoconferencia para todos si el usuario es anfitrion y marca la cita como finalizada en agenda.")
    public VideoMeeting end(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = getCurrentUserId(authentication);
        return videoMeetingService.end(id, currentUserId);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar videoconferencia", description = "Cancela la videoconferencia si el usuario tiene permisos.")
    public VideoMeeting cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelVideoMeetingRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = getCurrentUserId(authentication);
        return videoMeetingService.cancel(id, request, currentUserId);
    }
}
