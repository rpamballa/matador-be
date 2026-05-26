CREATE TABLE payment_method (
    id                       UUID PRIMARY KEY,
    customer_id              UUID NOT NULL REFERENCES customer (id),
    stripe_payment_method_id TEXT NOT NULL UNIQUE,
    type                     TEXT NOT NULL,
    brand                    TEXT,
    last4                    CHAR(4),
    exp_month                INT,
    exp_year                 INT,
    is_default               BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ NOT NULL,
    detached_at              TIMESTAMPTZ
);
CREATE INDEX idx_payment_method_customer ON payment_method (customer_id) WHERE detached_at IS NULL;

-- booking_id / trip_id are cross-module references kept as plain UUIDs (no DB FK) so the
-- payment module's schema stays independent of the booking/trip modules.
CREATE TABLE payment_intent (
    id                    UUID PRIMARY KEY,
    customer_id           UUID NOT NULL REFERENCES customer (id),
    booking_id            UUID,
    trip_id               UUID,
    stripe_intent_id      TEXT NOT NULL UNIQUE,
    purpose               TEXT NOT NULL,
    amount_cents          BIGINT NOT NULL,
    captured_amount_cents BIGINT NOT NULL DEFAULT 0,
    status                TEXT NOT NULL,
    last_error            TEXT,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_payment_intent_booking ON payment_intent (booking_id);
CREATE INDEX idx_payment_intent_trip ON payment_intent (trip_id);

-- Idempotency keys for Stripe calls.
CREATE TABLE payment_idempotency_key (
    id           UUID PRIMARY KEY,
    operation    TEXT NOT NULL,
    entity_id    UUID NOT NULL,
    attempt      INT NOT NULL DEFAULT 0,
    idem_key     TEXT NOT NULL UNIQUE,
    created_at   TIMESTAMPTZ NOT NULL,
    UNIQUE (operation, entity_id, attempt)
);
