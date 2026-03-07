package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepo extends JpaRepository<Notification, UUID> {}
