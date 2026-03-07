package com.upiiz.platform_api.services;

import com.upiiz.platform_api.entities.*;
import com.upiiz.platform_api.models.*;
import com.upiiz.platform_api.repositories.AppointmentRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AgendaService {

    private final AppointmentRepo appointmentRepo;
    private final ReminderService reminderService;
    private final NotificationService notificationService;

    public AgendaService(AppointmentRepo appointmentRepo, ReminderService reminderService, NotificationService notificationService) {
        this.appointmentRepo = appointmentRepo;
        this.reminderService = reminderService;
        this.notificationService = notificationService;
    }

    public List<Appointment> getAgenda(UUID currentUserId, LocalDateTime from, LocalDateTime to) {
        return appointmentRepo.findAgenda(currentUserId, from, to);
    }

    @Transactional
    public Appointment create(UUID currentUserId, CreateAppointmentCmd cmd) {
        validateTimes(cmd.startsAt(), cmd.endsAt);

        Appointment a = Appointment.create(currentUserId, cmd.title(), cmd.description(), cmd.modality(), cmd.startsAt(), cmd.endsAt());
        a.addParticipant(currentUserId, ParticipantRole.HOST, RSVPStatus.ACCEPTED);

        var invitees = cmd.inviteeUserIds() == null ? List.<UUID>of() : cmd.inviteeUserIds();

        for (UUID inviteeId : invitees) {
            // ...
            if (inviteeId.equals(currentUserId)) continue;
            a.addParticipant(inviteeId, ParticipantRole.ATTENDEE, RSVPStatus.PENDING);
        }

        Appointment saved = appointmentRepo.save(a);

        reminderService.scheduleForAppointment(saved);
        notificationService.notifyInvitees(saved);

        return saved;
    }

    @Transactional
    public Appointment reschedule(UUID currentUserId, UUID appointmentId, RescheduleCmd cmd) {
        validateTimes(cmd.startsAt(), cmd.endsAt);

        Appointment a = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        ensureEditable(a);
        if (!a.isHost(currentUserId)) throw new RuntimeException("Forbidden");

        a.reschedule(cmd.startsAt(), cmd.endsAt);

        reminderService.rescheduleForAppointment(a);
        notificationService.notifyRescheduled(a);

        return a;
    }

    @Transactional
    public void cancel(UUID currentUserId, UUID appointmentId, String reason) {
        Appointment a = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        ensureEditable(a);
        if (!a.isHost(currentUserId)) throw new RuntimeException("Forbidden");

        a.cancel();
        reminderService.cancelForAppointment(a.getId());
        notificationService.notifyCancelled(a, reason);
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) throw new RuntimeException("Invalid time range");
    }

    private void ensureEditable(Appointment a) {
        if (a.getStatus() != AppointmentStatus.SCHEDULED) throw new RuntimeException("Appointment not editable");
    }

    // Commands (records)
    public record CreateAppointmentCmd(String title, String description, Modality modality,
                                       LocalDateTime startsAt, LocalDateTime endsAt,
                                       List<UUID> inviteeUserIds) {}

    public record RescheduleCmd(LocalDateTime startsAt, LocalDateTime endsAt) {}
}
