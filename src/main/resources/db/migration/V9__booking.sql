CREATE SEQUENCE booking_number_seq START 1000;

CREATE TABLE booking (
    id                     UUID PRIMARY KEY,
    booking_number         TEXT NOT NULL UNIQUE,
    customer_id            UUID NOT NULL REFERENCES customer (id),
    vehicle_class_id       UUID NOT NULL REFERENCES vehicle_class (id),
    assigned_vehicle_id    UUID REFERENCES vehicle (id),
    pickup_at              TIMESTAMPTZ NOT NULL,
    dropoff_at             TIMESTAMPTZ NOT NULL,
    pickup_location        GEOGRAPHY(POINT, 4326) NOT NULL,
    pickup_address         TEXT NOT NULL,
    pickup_zone_id         UUID NOT NULL REFERENCES zone (id),
    dropoff_location       GEOGRAPHY(POINT, 4326) NOT NULL,
    dropoff_address        TEXT NOT NULL,
    dropoff_zone_id        UUID REFERENCES zone (id),
    dropoff_in_zone        BOOLEAN NOT NULL,
    quote_id               UUID NOT NULL REFERENCES pricing_quote (id),
    quoted_total_cents     BIGINT NOT NULL,
    deposit_hold_intent_id TEXT,
    deposit_amount_cents   BIGINT NOT NULL,
    insurance_tier         TEXT NOT NULL DEFAULT 'STANDARD',
    status                 TEXT NOT NULL,
    cancellation_reason    TEXT,
    cancelled_at           TIMESTAMPTZ,
    activated_at           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    created_by             UUID NOT NULL,
    updated_by             UUID
);
CREATE INDEX idx_booking_customer ON booking (customer_id);
CREATE INDEX idx_booking_vehicle ON booking (assigned_vehicle_id);
CREATE INDEX idx_booking_status_pickup ON booking (status, pickup_at);
CREATE INDEX idx_booking_class_window ON booking (vehicle_class_id, pickup_at, dropoff_at)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED', 'ACTIVATED');
