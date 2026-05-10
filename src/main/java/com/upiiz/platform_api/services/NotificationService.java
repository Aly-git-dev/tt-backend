package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.NotificationResponse;
import com.upiiz.platform_api.dto.PushNotificationPayload;
import com.upiiz.platform_api.entities.Appointment;
import com.upiiz.platform_api.entities.Notification;
import com.upiiz.platform_api.entities.Reminder;
import com.upiiz.platform_api.models.NotificationType;
import com.upiiz.platform_api.models.ParticipantRole;
import com.upiiz.platform_api.models.TargetType;
import com.upiiz.platform_api.repositories.NotificationRepository;
import com.upiiz.platform_api.repositories.UserRoleNativeRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final WebPushService webPushService;
    private final UserRoleNativeRepo userRoleRepo;

    public NotificationService(NotificationRepository notificationRepo,
                               WebPushService webPushService, UserRoleNativeRepo userRoleRepo) {
        this.notificationRepo = notificationRepo;
        this.webPushService = webPushService;
        this.userRoleRepo = userRoleRepo;
    }

    @Transactional
    public void notifyInvitees(Appointment a) {
        a.getParticipants().stream()
                .filter(p -> p.getRole() == ParticipantRole.ATTENDEE)
                .forEach(p -> {
                    String title = "Nueva cita";
                    String body = "Te invitaron a una cita: " + a.getTitle();

                    notificationRepo.save(Notification.of(
                            p.getUserId(),
                            NotificationType.INVITE,
                            title,
                            body,
                            TargetType.APPOINTMENT,
                            a.getId()
                    ));

                    sendBrowserPush(p.getUserId(), title, body, "/agenda");
                });
    }

    @Transactional
    public void notifyRescheduled(Appointment a) {
        a.getParticipants().forEach(p -> {
            String title = "Cita reprogramada";
            String body = "La cita \"" + a.getTitle() + "\" fue reprogramada.";

            notificationRepo.save(Notification.of(
                    p.getUserId(),
                    NotificationType.RESCHEDULED,
                    title,
                    body,
                    TargetType.APPOINTMENT,
                    a.getId()
            ));

            sendBrowserPush(p.getUserId(), title, body, "/agenda");
        });
    }

    @Transactional
    public void notifyCancelled(Appointment a, String reason) {
        String title = "Cita cancelada";
        String body = (reason == null || reason.isBlank())
                ? "La cita \"" + a.getTitle() + "\" fue cancelada."
                : "La cita \"" + a.getTitle() + "\" fue cancelada: " + reason;

        a.getParticipants().forEach(p -> {
            notificationRepo.save(Notification.of(
                    p.getUserId(),
                    NotificationType.CANCELLED,
                    title,
                    body,
                    TargetType.APPOINTMENT,
                    a.getId()
            ));

            sendBrowserPush(p.getUserId(), title, body, "/agenda");
        });
    }

    @Transactional
    public void notifyReminder(Reminder r) {
        String title = "Recordatorio de cita";
        String body = "Tienes una cita próxima.";

        notificationRepo.save(Notification.of(
                r.getUserId(),
                NotificationType.REMINDER,
                title,
                body,
                TargetType.APPOINTMENT,
                r.getTargetId()
        ));

        sendBrowserPush(r.getUserId(), title, body, "/agenda");
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listMine(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepo.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return page.map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        if (!n.getUserId().equals(userId)) {
            throw new RuntimeException("No puedes modificar esta notificación");
        }

        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now());
        }

        return NotificationResponse.from(notificationRepo.save(n));
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepo.findByUserIdAndReadAtIsNull(userId);

        LocalDateTime now = LocalDateTime.now();

        unread.forEach(n -> n.setReadAt(now));

        notificationRepo.saveAll(unread);
    }

    private void sendBrowserPush(UUID userId, String title, String body, String url) {
        try {
            webPushService.sendToUser(userId, new PushNotificationPayload(
                    title,
                    body,
                    "/assets/icons/icon-192x192.png",
                    url
            ));
        } catch (Exception e) {
            System.err.println("No se pudo enviar push al navegador: " + e.getMessage());
        }
    }

    @Transactional
    public void notifyForumReply(UUID userId, String authorName, Long threadId) {
        String title = "Nueva respuesta";
        String body = authorName + " respondió tu hilo";

        notificationRepo.save(Notification.of(
                userId,
                NotificationType.FORUM_REPLY,
                title,
                body,
                TargetType.THREAD,
                null
        ));

        sendBrowserPush(userId, title, body, "/forums/" + threadId);
    }

    @Transactional
    public void notifyForumLikeThread(UUID userId, String authorName, Long threadId) {
        String title = "Nuevo like";
        String body = authorName + " dio like a tu hilo";

        notificationRepo.save(Notification.of(
                userId,
                NotificationType.FORUM_LIKE,
                title,
                body,
                TargetType.THREAD,
                null
        ));

        sendBrowserPush(userId, title, body, "/forums/" + threadId);
    }

    @Transactional
    public void notifyForumLikePost(UUID userId, String authorName, Long threadId) {
        String title = "Nuevo like";
        String body = authorName + " dio like a tu respuesta";

        notificationRepo.save(Notification.of(
                userId,
                NotificationType.FORUM_LIKE,
                title,
                body,
                TargetType.THREAD,
                null
        ));

        sendBrowserPush(userId, title, body, "/forums/" + threadId);
    }

    @Transactional
    public void notifyForumReported(UUID userId) {
        String title = "Contenido reportado";
        String body = "Una de tus publicaciones fue reportada y será revisada";

        notificationRepo.save(Notification.of(
                userId,
                NotificationType.FORUM_REPORTED,
                title,
                body,
                TargetType.REPORT,
                null
        ));

        sendBrowserPush(userId, title, body, "/forums");
    }

    @Transactional
    public void notifyForumReportResolved(UUID userId) {
        String title = "Reporte revisado";
        String body = "Un administrador revisó un reporte sobre tu contenido";

        notificationRepo.save(Notification.of(
                userId,
                NotificationType.FORUM_REPORT_RESOLVED,
                title,
                body,
                TargetType.REPORT,
                null
        ));

        sendBrowserPush(userId, title, body, "/forums");
    }

    @Transactional
    public void notifyNewChatMessage(UUID userId, String senderName, UUID conversationId) {
        String title = "Nuevo mensaje";
        String body = senderName + " te envió un mensaje";

        notificationRepo.save(Notification.of(
                userId,
                NotificationType.NEW_MESSAGE,
                title,
                body,
                TargetType.CHAT_CONVERSATION,
                conversationId
        ));

        sendBrowserPush(userId, title, body, "/messages/" + conversationId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMessageReportSent(UUID userId, Long reportId) {
        String title = "Reporte enviado";
        String body = "Tu reporte fue enviado y será revisado por un administrador.";

        notificationRepo.save(Notification.of(
                userId,
                NotificationType.MESSAGE_REPORT_SENT,
                title,
                body,
                TargetType.CHAT_REPORT,
                null
        ));

        sendBrowserPush(userId, title, body, "/messages");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAdminsMessageReported(Long reportId) {
        String title = "Nuevo reporte de mensaje";
        String body = "Se recibió un nuevo reporte de mensaje que requiere revisión.";

        List<UUID> adminIds = userRoleRepo.findUserIdsByRoleName("ADMIN");

        for (UUID adminId : adminIds) {
            notificationRepo.save(Notification.of(
                    adminId,
                    NotificationType.MESSAGE_REPORTED_ADMIN,
                    title,
                    body,
                    TargetType.CHAT_REPORT,
                    null
            ));

            sendBrowserPush(adminId, title, body, "/admin/reports/messages");
        }
    }
}
