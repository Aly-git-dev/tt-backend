CREATE TABLE notification (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                              type VARCHAR(50) NOT NULL,
                              title VARCHAR(150) NOT NULL,
                              message TEXT NOT NULL,

                              reference_id VARCHAR(80),
                              reference_type VARCHAR(50),

                              is_read BOOLEAN NOT NULL DEFAULT FALSE,

                              created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                              CONSTRAINT ck_notification_type CHECK (
                                  type IN (
                                           'NEW_MESSAGE',
                                           'MESSAGE_REPORT_SENT',
                                           'MESSAGE_REPORTED_ADMIN',
                                           'REPORT_RESOLVED',
                                           'SANCTION_APPLIED'
                                      )
                                  )
);

CREATE INDEX idx_notification_user_created
    ON notification(user_id, created_at DESC);

CREATE INDEX idx_notification_user_read
    ON notification(user_id, is_read);

CREATE INDEX idx_notification_type
    ON notification(type);

DROP TRIGGER IF EXISTS set_timestamp_notification ON notification;
CREATE TRIGGER set_timestamp_notification
    BEFORE UPDATE ON notification
    FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();