CREATE TABLE video_meetings (
                                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                appointment_id  UUID NOT NULL UNIQUE
                                    REFERENCES appointments(id) ON DELETE CASCADE,

                                provider        VARCHAR(20) NOT NULL DEFAULT 'JITSI',
                                room_name       VARCHAR(180) NOT NULL UNIQUE,
                                meeting_url     TEXT NOT NULL,

                                host_user_id    UUID NOT NULL
                                    REFERENCES users(id) ON DELETE RESTRICT,

                                status          VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED | LIVE | ENDED | CANCELLED

                                created_by      UUID NOT NULL
                                    REFERENCES users(id) ON DELETE RESTRICT,

                                cancelled_by    UUID
                                    REFERENCES users(id) ON DELETE SET NULL,

                                cancel_reason   VARCHAR(500),
                                started_at      TIMESTAMP WITHOUT TIME ZONE,
                                ended_at        TIMESTAMP WITHOUT TIME ZONE,
                                cancelled_at    TIMESTAMP WITHOUT TIME ZONE,

                                created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                CONSTRAINT ck_video_meetings_provider
                                    CHECK (provider IN ('JITSI')),

                                CONSTRAINT ck_video_meetings_status
                                    CHECK (status IN ('SCHEDULED', 'LIVE', 'ENDED', 'CANCELLED')),

                                CONSTRAINT ck_video_meetings_dates
                                    CHECK (
                                        ended_at IS NULL
                                            OR started_at IS NULL
                                            OR ended_at >= started_at
                                        )
);

CREATE INDEX idx_video_meetings_host       ON video_meetings(host_user_id);
CREATE INDEX idx_video_meetings_status     ON video_meetings(status);
CREATE INDEX idx_video_meetings_created_by ON video_meetings(created_by);

DROP TRIGGER IF EXISTS set_timestamp_video_meetings ON video_meetings;
CREATE TRIGGER set_timestamp_video_meetings
    BEFORE UPDATE ON video_meetings
    FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();


CREATE TABLE video_meeting_attendance (
                                          id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          video_meeting_id UUID NOT NULL
                                              REFERENCES video_meetings(id) ON DELETE CASCADE,

                                          user_id          UUID NOT NULL
                                              REFERENCES users(id) ON DELETE CASCADE,

                                          role_in_meeting  VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT', -- HOST | PARTICIPANT
                                          joined_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                          left_at          TIMESTAMP WITHOUT TIME ZONE,
                                          session_seconds  INTEGER,
                                          device_info      VARCHAR(255),
                                          created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                          CONSTRAINT ck_video_meeting_attendance_role
                                              CHECK (role_in_meeting IN ('HOST', 'PARTICIPANT')),

                                          CONSTRAINT ck_video_meeting_attendance_dates
                                              CHECK (left_at IS NULL OR left_at >= joined_at)
);

CREATE INDEX idx_video_meeting_attendance_vm   ON video_meeting_attendance(video_meeting_id);
CREATE INDEX idx_video_meeting_attendance_user ON video_meeting_attendance(user_id);
CREATE OR REPLACE FUNCTION trg_validate_video_meeting_host()
RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM appointment_participants ap
        WHERE ap.appointment_id = NEW.appointment_id
          AND ap.user_id = NEW.host_user_id
    ) THEN
        RAISE EXCEPTION 'El host debe pertenecer a la cita';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS validate_video_meeting_host ON video_meetings;
CREATE TRIGGER validate_video_meeting_host
    BEFORE INSERT OR UPDATE ON video_meetings
                         FOR EACH ROW EXECUTE FUNCTION trg_validate_video_meeting_host();
CREATE OR REPLACE FUNCTION trg_validate_video_meeting_appointment()
RETURNS trigger AS $$
DECLARE
v_modality VARCHAR(20);
    v_status   VARCHAR(20);
BEGIN
SELECT modality, status
INTO v_modality, v_status
FROM appointments
WHERE id = NEW.appointment_id;

IF v_modality <> 'ONLINE' THEN
        RAISE EXCEPTION 'Solo se puede crear videoconferencia para citas ONLINE';
END IF;

    IF v_status = 'CANCELLED' THEN
        RAISE EXCEPTION 'No se puede crear videoconferencia para una cita cancelada';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS validate_video_meeting_appointment ON video_meetings;
CREATE TRIGGER validate_video_meeting_appointment
    BEFORE INSERT OR UPDATE ON video_meetings
                         FOR EACH ROW EXECUTE FUNCTION trg_validate_video_meeting_appointment();