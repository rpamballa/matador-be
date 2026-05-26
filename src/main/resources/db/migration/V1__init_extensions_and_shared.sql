-- Extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Staff users (admin API, session authentication)
CREATE TABLE staff_user (
    id            UUID PRIMARY KEY,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    first_name    TEXT NOT NULL,
    last_name     TEXT NOT NULL,
    role          TEXT NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    created_by    UUID,
    updated_by    UUID
);
CREATE INDEX idx_staff_user_email ON staff_user (LOWER(email));

-- Inbound webhook events: stored for audit and idempotency across providers.
CREATE TABLE webhook_event (
    id                UUID PRIMARY KEY,
    provider          TEXT NOT NULL,
    provider_event_id TEXT NOT NULL,
    event_type        TEXT,
    payload           JSONB,
    received_at       TIMESTAMPTZ NOT NULL,
    processed_at      TIMESTAMPTZ,
    UNIQUE (provider, provider_event_id)
);

-- ShedLock table for single-instance scheduled jobs.
CREATE TABLE shedlock (
    name       VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at  TIMESTAMPTZ NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
