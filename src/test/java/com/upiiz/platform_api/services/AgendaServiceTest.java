package com.upiiz.platform_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upiiz.platform_api.entities.Appointment;
import com.upiiz.platform_api.models.Modality;
import com.upiiz.platform_api.models.ParticipantRole;
import com.upiiz.platform_api.repositories.AppointmentRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    @Mock
    private AppointmentRepo appointmentRepo;

    @Mock
    private ReminderService reminderService;

    @Mock
    private NotificationService notificationService;

    @Test
    void createAddsMultipleInviteesAndSkipsDuplicatesAndHost() {
        AgendaService service = new AgendaService(appointmentRepo, reminderService, notificationService);
        UUID host = UUID.randomUUID();
        UUID invitee1 = UUID.randomUUID();
        UUID invitee2 = UUID.randomUUID();
        LocalDateTime startsAt = LocalDateTime.now().plusDays(1);
        LocalDateTime endsAt = startsAt.plusHours(1);

        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment saved = service.create(host, new AgendaService.CreateAppointmentCmd(
                "Asesoria",
                "Revision de avance",
                Modality.ONLINE,
                startsAt,
                endsAt,
                List.of(invitee1, invitee2, invitee1, host)
        ));

        assertEquals(3, saved.getParticipants().size());
        assertTrue(saved.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(host) && p.getRole() == ParticipantRole.HOST));
        assertTrue(saved.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(invitee1) && p.getRole() == ParticipantRole.ATTENDEE));
        assertTrue(saved.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(invitee2) && p.getRole() == ParticipantRole.ATTENDEE));

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepo).save(appointmentCaptor.capture());
        verify(reminderService).scheduleForAppointment(appointmentCaptor.getValue());
        verify(notificationService).notifyInvitees(appointmentCaptor.getValue());
    }

    @Test
    void createRejectsAppointmentsLongerThanTwoHours() {
        AgendaService service = new AgendaService(appointmentRepo, reminderService, notificationService);
        UUID host = UUID.randomUUID();
        LocalDateTime startsAt = LocalDateTime.now().plusDays(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(
                host,
                new AgendaService.CreateAppointmentCmd(
                        "Asesoria",
                        "Revision de avance",
                        Modality.ONLINE,
                        startsAt,
                        startsAt.plusHours(2).plusMinutes(1),
                        List.of()
                )
        ));

        assertEquals("La cita no puede durar mas de 2 horas", ex.getMessage());
        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void createAppointmentCmdAcceptsInvitadosAlias() throws Exception {
        UUID invitee1 = UUID.randomUUID();
        UUID invitee2 = UUID.randomUUID();
        String json = """
                {
                  "title": "Asesoria",
                  "description": "Revision",
                  "modality": "ONLINE",
                  "startsAt": "2026-06-08T10:00:00",
                  "endsAt": "2026-06-08T11:00:00",
                  "invitados": ["%s", "%s"]
                }
                """.formatted(invitee1, invitee2);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        AgendaService.CreateAppointmentCmd cmd = mapper.readValue(json, AgendaService.CreateAppointmentCmd.class);

        assertEquals(List.of(invitee1, invitee2), cmd.inviteeUserIds());
    }

    @Test
    void createAppointmentCmdAcceptsSingleInviteeAlias() throws Exception {
        UUID invitee = UUID.randomUUID();
        String json = """
                {
                  "title": "Asesoria",
                  "description": "Revision",
                  "modality": "ONLINE",
                  "startsAt": "2026-06-08T10:00:00",
                  "endsAt": "2026-06-08T11:00:00",
                  "inviteeUserId": "%s"
                }
                """.formatted(invitee);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        AgendaService.CreateAppointmentCmd cmd = mapper.readValue(json, AgendaService.CreateAppointmentCmd.class);

        assertEquals(List.of(invitee), cmd.inviteeUserIds());
    }
}
