CREATE TABLE zone (
    id                            UUID PRIMARY KEY,
    name                          TEXT NOT NULL UNIQUE,
    slug                          TEXT NOT NULL UNIQUE,
    boundary                      GEOGRAPHY(POLYGON, 4326) NOT NULL,
    center                        GEOGRAPHY(POINT, 4326) NOT NULL,
    out_of_zone_dropoff_fee_cents BIGINT NOT NULL DEFAULT 0,
    out_of_zone_dropoff_allowed   BOOLEAN NOT NULL DEFAULT TRUE,
    active                        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                    TIMESTAMPTZ NOT NULL,
    updated_at                    TIMESTAMPTZ NOT NULL,
    created_by                    UUID,
    updated_by                    UUID
);
CREATE INDEX idx_zone_boundary ON zone USING GIST (boundary);

-- Seed the single Phase 1 zone: the Triangle (Raleigh-Durham, NC), approximate boundary.
INSERT INTO zone (
    id, name, slug, boundary, center,
    out_of_zone_dropoff_fee_cents, out_of_zone_dropoff_allowed, active, created_at, updated_at)
VALUES (
    '018f0000-0000-7000-8000-000000000001',
    'Triangle',
    'triangle',
    ST_GeogFromText('SRID=4326;POLYGON((-79.05 36.10, -78.50 36.10, -78.50 35.70, -79.05 35.70, -79.05 36.10))'),
    ST_GeogFromText('SRID=4326;POINT(-78.78 35.90)'),
    2500,
    TRUE,
    TRUE,
    now(),
    now()
);
