package com.upiiz.platform_api.services;

import com.upiiz.platform_api.entities.*;
import com.upiiz.platform_api.models.*;
import com.upiiz.platform_api.repositories.NotificationRepo;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepo notificationRepo;

    public NotificationService(NotificationRepo notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    public void notifyInvitees(Appointment a) {
        a.getParticipants().forEach(p -> {
            if (p.getRole() == ParticipantRole.ATTENDEE) {
                notificationRepo.save(Notification.of(
                        p.getUserId(),
                        NotificationType.INVITE,
                        "Nueva cita",
                        "Te invitaron a una cita: " + a.getId(),
                        TargetType.APPOINTMENT,
                        a.getId()
                ));
            }
        });
    }

    public void notifyRescheduled(Appointment a) {
        a.getParticipants().forEach(p -> notificationRepo.save(Notification.of(
                p.getUserId(), NotificationType.RESCHEDULED,
                "Cita reprogramada", "La cita fue reprogramada.",
                TargetType.APPOINTMENT, a.getId()
        )));
    }

    public void notifyCancelled(Appointment a, String reason) {
        String body = (reason == null || reason.isBlank()) ? "La cita fue cancelada." : ("La cita fue cancelada: " + reason);
        a.getParticipants().forEach(p -> notificationRepo.save(Notification.of(
                p.getUserId(), NotificationType.CANCELLED,
                "Cita cancelada", body,
                TargetType.APPOINTMENT, a.getId()
        )));
    }

    public void notifyReminder(Reminder r) {
        notificationRepo.save(Notification.of(
                r.getUserId(), NotificationType.REMINDER,
                "Recordatorio de cita", "Tienes una cita próxima.",
                TargetType.APPOINTMENT, r.getTargetId()
        ));
    }
}
