package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.Reminder;
import com.upiiz.platform_api.models.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface ReminderRepo extends JpaRepository<Reminder, UUID> {
    List<Reminder> findTop200BySentAtIsNullAndRemindAtLessThanEqual(LocalDateTime now);
    void deleteByTargetTypeAndTargetId(TargetType targetType, UUID targetId);
}
