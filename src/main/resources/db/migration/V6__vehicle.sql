CREATE TABLE vehicle_class (
    id                   UUID PRIMARY KEY,
    name                 TEXT NOT NULL UNIQUE,
    description          TEXT,
    seats                INT NOT NULL,
    luggage_capacity     INT NOT NULL,
    drivetrain           TEXT NOT NULL,
    base_daily_rate_cents BIGINT NOT NULL,
    sort_order           INT NOT NULL DEFAULT 0,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    created_by           UUID,
    updated_by           UUID
);

CREATE TABLE vehicle (
    id                   UUID PRIMARY KEY,
    vin                  TEXT NOT NULL UNIQUE,
    license_plate        TEXT NOT NULL,
    license_state        CHAR(2) NOT NULL,
    make                 TEXT NOT NULL,
    model                TEXT NOT NULL,
    year                 INT NOT NULL,
    color                TEXT NOT NULL,
    class_id             UUID NOT NULL REFERENCES vehicle_class (id),
    status               TEXT NOT NULL,
    current_location     GEOGRAPHY(POINT, 4326),
    current_address      TEXT,
    odometer_miles       INT NOT NULL DEFAULT 0,
    fuel_charge_percent  INT,
    range_miles          INT,
    home_zone_id         UUID NOT NULL REFERENCES zone (id),
    telematics_provider  TEXT,
    telematics_vehicle_id TEXT,
    notes                TEXT,
    acquired_on          DATE NOT NULL,
    retired_on           DATE,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    created_by           UUID,
    updated_by           UUID
);
CREATE INDEX idx_vehicle_status ON vehicle (status);
CREATE INDEX idx_vehicle_class ON vehicle (class_id);
CREATE INDEX idx_vehicle_location ON vehicle USING GIST (current_location);

CREATE TABLE vehicle_photo (
    id         UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES vehicle (id),
    url        TEXT NOT NULL,
    label      TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_vehicle_photo_vehicle ON vehicle_photo (vehicle_id);
