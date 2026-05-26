-- booking_id / customer_id / vehicle_id / *_inspection_id are cross-module references
-- kept as plain UUIDs (no DB FK).
CREATE TABLE trip (
    id                       UUID PRIMARY KEY,
    booking_id               UUID NOT NULL UNIQUE,
    customer_id              UUID NOT NULL,
    vehicle_id               UUID NOT NULL,
    status                   TEXT NOT NULL,
    actual_pickup_at         TIMESTAMPTZ NOT NULL,
    actual_dropoff_at        TIMESTAMPTZ,
    actual_pickup_location   GEOGRAPHY(POINT, 4326) NOT NULL,
    actual_dropoff_location  GEOGRAPHY(POINT, 4326),
    actual_dropoff_address   TEXT,
    actual_dropoff_in_zone   BOOLEAN,
    odometer_start           INT NOT NULL,
    odometer_end             INT,
    miles_driven             INT,
    quoted_total_cents       BIGINT NOT NULL,
    pickup_inspection_id     UUID,
    dropoff_inspection_id    UUID,
    final_charges_cents      BIGINT,
    closed_at                TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_trip_customer ON trip (customer_id);
CREATE INDEX idx_trip_vehicle ON trip (vehicle_id);
CREATE INDEX idx_trip_status ON trip (status);

CREATE TABLE trip_location_sample (
    id                  UUID PRIMARY KEY,
    trip_id             UUID NOT NULL REFERENCES trip (id),
    sampled_at          TIMESTAMPTZ NOT NULL,
    location            GEOGRAPHY(POINT, 4326) NOT NULL,
    speed_mph           NUMERIC(5,2),
    odometer_miles      INT,
    fuel_charge_percent INT
);
CREATE INDEX idx_trip_sample_trip_time ON trip_location_sample (trip_id, sampled_at);
