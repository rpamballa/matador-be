CREATE TABLE verification_session (
    id                  UUID PRIMARY KEY,
    customer_id         UUID NOT NULL REFERENCES customer (id),
    provider            TEXT NOT NULL DEFAULT 'STRIPE_IDENTITY',
    provider_session_id TEXT NOT NULL UNIQUE,
    client_secret       TEXT NOT NULL,
    status              TEXT NOT NULL,
    result_payload      JSONB,
    created_at          TIMESTAMPTZ NOT NULL,
    completed_at        TIMESTAMPTZ
);
CREATE INDEX idx_verification_customer ON verification_session (customer_id);
