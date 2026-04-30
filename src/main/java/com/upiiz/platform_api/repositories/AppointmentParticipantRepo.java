package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.AppointmentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentParticipantRepo
        extends JpaRepository<AppointmentParticipant, AppointmentParticipant.PK> {

    boolean existsByAppointment_IdAndUserId(UUID appointmentId, UUID userId);

    List<Object[]> findAdminTopicInterest();
}