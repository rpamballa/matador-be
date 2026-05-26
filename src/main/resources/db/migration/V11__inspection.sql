-- trip_id references the trip module's aggregate (plain UUID, no cross-module FK).
CREATE TABLE inspection (
    id                UUID PRIMARY KEY,
    trip_id           UUID NOT NULL,
    phase             TEXT NOT NULL,
    odometer_miles    INT,
    fuel_charge_percent INT,
    notes             TEXT,
    submitted_at      TIMESTAMPTZ NOT NULL,
    submitted_by_role TEXT NOT NULL,
    submitted_by_id   UUID NOT NULL,
    reviewed_at       TIMESTAMPTZ,
    reviewed_by       UUID,
    review_status     TEXT,
    review_notes      TEXT
);
CREATE INDEX idx_inspection_trip ON inspection (trip_id, phase);

CREATE TABLE inspection_photo (
    id             UUID PRIMARY KEY,
    inspection_id  UUID NOT NULL REFERENCES inspection (id),
    angle          TEXT NOT NULL,
    url            TEXT NOT NULL,
    captured_at    TIMESTAMPTZ NOT NULL,
    location       GEOGRAPHY(POINT, 4326),
    file_size_bytes INT,
    width_px       INT,
    height_px      INT
);
CREATE INDEX idx_inspection_photo_inspection ON inspection_photo (inspection_id);
