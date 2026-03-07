package com.upiiz.platform_api.services;

import com.upiiz.platform_api.entities.*;
import com.upiiz.platform_api.models.*;
import com.upiiz.platform_api.repositories.ReminderRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReminderService {

    private final ReminderRepo reminderRepo;

    public ReminderService(ReminderRepo reminderRepo) {
        this.reminderRepo = reminderRepo;
    }

    @Transactional
    public void scheduleForAppointment(Appointment a) {
        // policy: 24h y 1h antes
        a.getParticipants().forEach(p -> {
            // por ahora: IN_APP (si luego tienes push permissions, aquí decides)
            create(a.getId(), p.getUserId(), ReminderChannel.IN_APP, a.getStartsAt().minusHours(24));
            create(a.getId(), p.getUserId(), ReminderChannel.IN_APP, a.getStartsAt().minusHours(1));
        });
    }

    @Transactional
    public void rescheduleForAppointment(Appointment a) {
        cancelForAppointment(a.getId());
        scheduleForAppointment(a);
    }

    @Transactional
    public void cancelForAppointment(java.util.UUID appointmentId) {
        reminderRepo.deleteByTargetTypeAndTargetId(TargetType.APPOINTMENT, appointmentId);
    }

    private void create(java.util.UUID appointmentId, java.util.UUID userId, ReminderChannel channel, LocalDateTime remindAt) {
        // si remindAt ya pasó (ej. cita en 30 min), igual lo guardas y el job lo dispara inmediato
        reminderRepo.save(Reminder.create(TargetType.APPOINTMENT, appointmentId, userId, channel, remindAt));
    }
}
