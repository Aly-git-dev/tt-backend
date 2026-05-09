package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    List<EmailVerification> findByUserIdAndUsedFalse(UUID userId);
}
