package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.services.AgendaService;
import com.upiiz.platform_api.services.AgendaService.CreateAppointmentCmd;
import com.upiiz.platform_api.services.AgendaService.RescheduleCmd;
import com.upiiz.platform_api.services.UserSearchService;
import com.upiiz.platform_api.dto.ApiResponse;
import com.upiiz.platform_api.dto.UserSearchResponse;
import com.upiiz.platform_api.entities.Appointment;
import com.upiiz.platform_api.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/upiiz/public/v1/agenda")
@Tag(name = "Agenda", description = "Gestion de citas, reprogramaciones y cancelaciones")
@SecurityRequirement(name = "bearer-jwt")
public class AgendaController {

    private final AgendaService agendaService;
    private final UserSearchService userSearchService;

    public AgendaController(AgendaService agendaService, UserSearchService userSearchService) {
        this.agendaService = agendaService;
        this.userSearchService = userSearchService;
    }

    @GetMapping()
    @Operation(summary = "Consultar agenda", description = "Devuelve las citas del usuario autenticado dentro de un rango de fechas.")
    public ResponseEntity<?> getAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        try {
            UUID userId = CurrentUser.id();
            return ResponseEntity.ok(agendaService.getAgenda(userId, from, to));
        } catch (Exception e) {
            e.printStackTrace(); // 👈 esto fuerza que salga en consola
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/appointments")
    @Operation(summary = "Crear cita", description = "Crea una cita y notifica a los invitados.")
    public Appointment create(@RequestBody CreateAppointmentCmd cmd) {
        UUID userId = CurrentUser.id();
        return agendaService.create(userId, cmd);
    }

    @GetMapping("/teachers/search")
    @Operation(summary = "Buscar docentes", description = "Busca docentes activos por nombre o correo para crear citas.")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> searchTeachers(
            @RequestParam(required = false, defaultValue = "") String q
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Docentes encontrados correctamente", userSearchService.searchTeachers(q))
        );
    }

    @PutMapping("/appointments/{id}")
    @Operation(summary = "Reprogramar cita", description = "Actualiza el horario de una cita creada por el usuario anfitrion.")
    public Appointment reschedule(@PathVariable UUID id, @RequestBody RescheduleCmd cmd) {
        UUID userId = CurrentUser.id();
        return agendaService.reschedule(userId, id, cmd);
    }

    public record CancelBody(String reason) {}

    @PostMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancelar cita", description = "Cancela una cita y notifica a sus participantes.")
    public void cancel(@PathVariable UUID id, @RequestBody(required = false) CancelBody body) {
        UUID userId = CurrentUser.id();
        agendaService.cancel(userId, id, body == null ? null : body.reason());
    }
}
