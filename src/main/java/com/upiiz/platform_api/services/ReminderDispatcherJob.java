package com.upiiz.platform_api.services;

import com.upiiz.platform_api.repositories.ReminderRepo;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReminderDispatcherJob {

    private final ReminderRepo reminderRepo;
    private final NotificationService notificationService;

    public ReminderDispatcherJob(ReminderRepo reminderRepo, NotificationService notificationService) {
        this.reminderRepo = reminderRepo;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60000) // cada 60s
    @Transactional
    public void dispatch() {
        var due = reminderRepo.findTop200BySentAtIsNullAndRemindAtLessThanEqual(LocalDateTime.now());
        for (var r : due) {
            notificationService.notifyReminder(r);
            r.markSent();
        }
    }
}
