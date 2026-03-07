package com.upiiz.platform_api.entities;

import com.upiiz.platform_api.models.*;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointment_participants")
@IdClass(AppointmentParticipant.PK.class)
public class AppointmentParticipant {

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Id
    @Column(name="user_id", nullable=false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private RSVPStatus rsvp;

    protected AppointmentParticipant() {}

    public AppointmentParticipant(Appointment appointment, UUID userId, ParticipantRole role, RSVPStatus rsvp) {
        this.appointment = appointment;
        this.userId = userId;
        this.role = role;
        this.rsvp = rsvp;
    }

    public UUID getUserId() { return userId; }
    public ParticipantRole getRole() { return role; }

    public static class PK implements Serializable {
        private UUID appointment;
        private UUID userId;

        public PK() {}
        public PK(UUID appointment, UUID userId) { this.appointment = appointment; this.userId = userId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(appointment, pk.appointment) && Objects.equals(userId, pk.userId);
        }
        @Override public int hashCode() { return Objects.hash(appointment, userId); }
    }
}
