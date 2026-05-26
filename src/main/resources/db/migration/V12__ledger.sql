-- Immutable financial ledger. Insert-only: no UPDATE/DELETE; corrections via compensating
-- entries. Cross-module references are plain UUIDs to keep module schemas independent.
CREATE TABLE ledger_entry (
    id                UUID PRIMARY KEY,
    occurred_at       TIMESTAMPTZ NOT NULL,
    customer_id       UUID NOT NULL REFERENCES customer (id),
    booking_id        UUID,
    trip_id           UUID,
    incident_id       UUID,
    entry_type        TEXT NOT NULL,
    amount_cents      BIGINT NOT NULL,
    currency          CHAR(3) NOT NULL DEFAULT 'USD',
    description       TEXT NOT NULL,
    payment_intent_id UUID,
    metadata          JSONB,
    created_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ledger_customer ON ledger_entry (customer_id, occurred_at);
CREATE INDEX idx_ledger_booking ON ledger_entry (booking_id);
CREATE INDEX idx_ledger_trip ON ledger_entry (trip_id);
