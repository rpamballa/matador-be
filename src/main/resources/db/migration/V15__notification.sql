CREATE TABLE notification (
    id          UUID PRIMARY KEY,
    customer_id UUID REFERENCES customer (id),
    channel     TEXT NOT NULL,
    template    TEXT NOT NULL,
    payload     JSONB NOT NULL,
    recipient   TEXT NOT NULL,
    status      TEXT NOT NULL,
    sent_at     TIMESTAMPTZ,
    error       TEXT,
    created_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_notification_status ON notification (status);
