package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    List<PushSubscription> findByUserIdAndActiveTrue(UUID userId);

    Optional<PushSubscription> findByEndpoint(String endpoint);
}