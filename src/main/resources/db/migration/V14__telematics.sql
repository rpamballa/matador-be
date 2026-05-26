CREATE TABLE telematics_token (
    id                       UUID PRIMARY KEY,
    vehicle_id               UUID NOT NULL REFERENCES vehicle (id),
    provider                 TEXT NOT NULL,
    access_token             TEXT NOT NULL,
    refresh_token            TEXT NOT NULL,
    access_token_expires_at  TIMESTAMPTZ NOT NULL,
    scopes                   TEXT NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX idx_telematics_token_vehicle ON telematics_token (vehicle_id, provider);

CREATE TABLE telematics_command_log (
    id               UUID PRIMARY KEY,
    vehicle_id       UUID NOT NULL REFERENCES vehicle (id),
    trip_id          UUID,
    command          TEXT NOT NULL,
    requested_at     TIMESTAMPTZ NOT NULL,
    requested_by     UUID NOT NULL,
    succeeded        BOOLEAN,
    error            TEXT,
    response_payload JSONB
);
CREATE INDEX idx_telematics_command_vehicle ON telematics_command_log (vehicle_id);
