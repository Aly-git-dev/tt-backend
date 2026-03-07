package com.upiiz.platform_api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.upiiz.platform_api.models.*;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modality modality;

    @Column(name="starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name="ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name="created_by", nullable = false)
    private UUID createdBy;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Getter
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<AppointmentParticipant> participants = new HashSet<>();

    protected Appointment() {}

    public static Appointment create(UUID creatorId, String title, String description, Modality modality,
                                     LocalDateTime startsAt, LocalDateTime endsAt) {
        Appointment a = new Appointment();
        a.id = UUID.randomUUID();
        a.createdBy = creatorId;
        a.title = title;
        a.description = description;
        a.modality = modality;
        a.startsAt = startsAt;
        a.endsAt = endsAt;
        a.status = AppointmentStatus.SCHEDULED;
        a.createdAt = LocalDateTime.now();
        a.updatedAt = a.createdAt;
        return a;
    }

    public void addParticipant(UUID userId, ParticipantRole role, RSVPStatus rsvp) {
        AppointmentParticipant p = new AppointmentParticipant(this, userId, role, rsvp);
        participants.add(p);
    }

    public boolean isHost(UUID userId) {
        return participants.stream().anyMatch(p -> p.getUserId().equals(userId) && p.getRole() == ParticipantRole.HOST);
    }

    public void reschedule(LocalDateTime startsAt, LocalDateTime endsAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // getters (o Lombok si usas)
    public UUID getId() { return id; }
    public UUID getCreatedBy() { return createdBy; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Modality getModality() { return modality; }
}
