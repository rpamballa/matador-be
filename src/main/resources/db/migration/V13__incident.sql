-- trip_id / vehicle_id / customer_id are cross-module references (plain UUIDs).
CREATE TABLE incident (
    id                   UUID PRIMARY KEY,
    trip_id              UUID,
    vehicle_id           UUID NOT NULL,
    customer_id          UUID,
    type                 TEXT NOT NULL,
    severity             TEXT NOT NULL,
    description          TEXT NOT NULL,
    occurred_at          TIMESTAMPTZ NOT NULL,
    reported_at          TIMESTAMPTZ NOT NULL,
    reported_by_role     TEXT NOT NULL,
    reported_by_id       UUID NOT NULL,
    location             GEOGRAPHY(POINT, 4326),
    status               TEXT NOT NULL,
    resolution_notes     TEXT,
    resolved_at          TIMESTAMPTZ,
    resolved_by          UUID,
    charged_amount_cents BIGINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_incident_trip ON incident (trip_id);
CREATE INDEX idx_incident_status ON incident (status);

CREATE TABLE incident_photo (
    id          UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incident (id),
    url         TEXT NOT NULL,
    caption     TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL
);
