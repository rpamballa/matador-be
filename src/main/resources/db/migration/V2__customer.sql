CREATE TABLE customer (
    id                        UUID PRIMARY KEY,
    email                     TEXT NOT NULL UNIQUE,
    phone                     TEXT NOT NULL UNIQUE,
    -- password_hash is not in the BACKEND.md table sketch but is required by the
    -- register/login endpoints; added here as the credential store.
    password_hash             TEXT NOT NULL,
    first_name                TEXT NOT NULL,
    last_name                 TEXT NOT NULL,
    date_of_birth             DATE NOT NULL,
    license_number            TEXT,
    license_state             CHAR(2),
    license_expires_on        DATE,
    verification_status       TEXT NOT NULL,
    verification_completed_at TIMESTAMPTZ,
    stripe_customer_id        TEXT UNIQUE,
    status                    TEXT NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL,
    updated_at                TIMESTAMPTZ NOT NULL,
    created_by                UUID,
    updated_by                UUID
);
CREATE INDEX idx_customer_email ON customer (LOWER(email));
CREATE INDEX idx_customer_phone ON customer (phone);
CREATE INDEX idx_customer_verification ON customer (verification_status);

CREATE TABLE customer_address (
    id          UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer (id),
    label       TEXT,
    line1       TEXT NOT NULL,
    line2       TEXT,
    city        TEXT NOT NULL,
    state       CHAR(2) NOT NULL,
    postal_code TEXT NOT NULL,
    country     CHAR(2) NOT NULL DEFAULT 'US',
    location    GEOGRAPHY(POINT, 4326) NOT NULL,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_customer_address_customer ON customer_address (customer_id);
