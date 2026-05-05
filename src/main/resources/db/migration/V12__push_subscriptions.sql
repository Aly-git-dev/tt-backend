CREATE TABLE push_subscriptions (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                    endpoint TEXT NOT NULL,
                                    p256dh TEXT NOT NULL,
                                    auth TEXT NOT NULL,

                                    user_agent TEXT,
                                    active BOOLEAN NOT NULL DEFAULT TRUE,

                                    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                    CONSTRAINT ux_push_subscription_endpoint UNIQUE (endpoint)
);

CREATE INDEX idx_push_subscriptions_user ON push_subscriptions(user_id);
CREATE INDEX idx_push_subscriptions_active ON push_subscriptions(active);

DROP TRIGGER IF EXISTS set_timestamp_push_subscriptions ON push_subscriptions;
CREATE TRIGGER set_timestamp_push_subscriptions
    BEFORE UPDATE ON push_subscriptions
    FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();