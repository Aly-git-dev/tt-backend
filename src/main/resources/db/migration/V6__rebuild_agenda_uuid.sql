-- V6__rebuild_agenda_uuid.sql
-- Objetivo: alinear Agenda a UUID (users.id es UUID)
-- Nota: esta migración elimina tablas de agenda existentes y las recrea.

-- 1) Primero tiramos dependencias en orden correcto
DROP TABLE IF EXISTS appointment_participants;
DROP TABLE IF EXISTS reminders;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS appointments;

-- 2) Recrear appointments con UUID y FK a users
CREATE TABLE appointments (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              title VARCHAR(120) NOT NULL,
                              description TEXT,
                              modality VARCHAR(20) NOT NULL, -- ONLINE | PRESENCIAL
                              starts_at TIMESTAMP NOT NULL,
                              ends_at TIMESTAMP NOT NULL,
                              status VARCHAR(20) NOT NULL,   -- SCHEDULED | CANCELLED | COMPLETED
                              created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                              created_at TIMESTAMP NOT NULL DEFAULT now(),
                              updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- (opcional pero recomendado) trigger updated_at para appointments
DROP TRIGGER IF EXISTS set_timestamp_appointments ON appointments;
CREATE TRIGGER set_timestamp_appointments
    BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();

-- 3) appointment_participants con UUID y FKs correctas
CREATE TABLE appointment_participants (
                                          appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
                                          user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          role VARCHAR(20) NOT NULL,  -- HOST | ATTENDEE
                                          rsvp VARCHAR(20) NOT NULL,  -- PENDING | ACCEPTED | DECLINED | TENTATIVE
                                          PRIMARY KEY (appointment_id, user_id)
);

CREATE INDEX idx_participants_user ON appointment_participants (user_id);

-- 4) reminders con UUID (target_id = appointment_id)
CREATE TABLE reminders (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           target_type VARCHAR(20) NOT NULL, -- APPOINTMENT
                           target_id UUID NOT NULL,          -- appointments.id
                           user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           channel VARCHAR(20) NOT NULL,     -- IN_APP | PUSH
                           remind_at TIMESTAMP NOT NULL,
                           sent_at TIMESTAMP NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminders_due ON reminders (remind_at) WHERE sent_at IS NULL;
CREATE INDEX idx_reminders_user ON reminders (user_id);

-- 5) notifications con UUID (target_id = appointment_id)
CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type VARCHAR(40) NOT NULL,         -- INVITE | RESCHEDULED | CANCELLED | REMINDER
                               title VARCHAR(140) NOT NULL,
                               body TEXT,
                               target_type VARCHAR(20),
                               target_id UUID,
                               read_at TIMESTAMP NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications (user_id, read_at);
