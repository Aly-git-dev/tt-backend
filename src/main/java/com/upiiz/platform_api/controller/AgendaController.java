package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.services.AgendaService;
import com.upiiz.platform_api.services.AgendaService.CreateAppointmentCmd;
import com.upiiz.platform_api.services.AgendaService.RescheduleCmd;
import com.upiiz.platform_api.entities.Appointment;
import com.upiiz.platform_api.security.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/upiiz/public/v1/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping()
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
    public Appointment create(@RequestBody CreateAppointmentCmd cmd) {
        UUID userId = CurrentUser.id();
        return agendaService.create(userId, cmd);
    }

    @PutMapping("/appointments/{id}")
    public Appointment reschedule(@PathVariable UUID id, @RequestBody RescheduleCmd cmd) {
        UUID userId = CurrentUser.id();
        return agendaService.reschedule(userId, id, cmd);
    }

    public record CancelBody(String reason) {}

    @PostMapping("/appointments/{id}/cancel")
    public void cancel(@PathVariable UUID id, @RequestBody(required = false) CancelBody body) {
        UUID userId = CurrentUser.id();
        agendaService.cancel(userId, id, body == null ? null : body.reason());
    }
}
