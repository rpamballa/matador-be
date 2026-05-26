CREATE TABLE pricing_rate (
    id                            UUID PRIMARY KEY,
    vehicle_class_id              UUID NOT NULL,
    daily_rate_cents              BIGINT NOT NULL,
    delivery_fee_cents            BIGINT NOT NULL DEFAULT 0,
    out_of_zone_dropoff_fee_cents BIGINT NOT NULL DEFAULT 0,
    insurance_tier                TEXT NOT NULL,
    insurance_daily_cents         BIGINT NOT NULL,
    effective_from                TIMESTAMPTZ NOT NULL,
    effective_to                  TIMESTAMPTZ,
    created_at                    TIMESTAMPTZ NOT NULL,
    created_by                    UUID NOT NULL
);
CREATE INDEX idx_rate_class_active ON pricing_rate (vehicle_class_id, insurance_tier, effective_from);

CREATE TABLE promo_code (
    id             UUID PRIMARY KEY,
    code           TEXT NOT NULL UNIQUE,
    discount_type  TEXT NOT NULL,
    discount_value INT NOT NULL,
    max_uses       INT,
    used_count     INT NOT NULL DEFAULT 0,
    starts_at      TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE pricing_quote (
    id               UUID PRIMARY KEY,
    customer_id      UUID,
    vehicle_class_id UUID NOT NULL,
    pickup_at        TIMESTAMPTZ NOT NULL,
    dropoff_at       TIMESTAMPTZ NOT NULL,
    dropoff_in_zone  BOOLEAN NOT NULL,
    insurance_tier   TEXT NOT NULL DEFAULT 'STANDARD',
    line_items       JSONB NOT NULL,
    subtotal_cents   BIGINT NOT NULL,
    tax_cents        BIGINT NOT NULL,
    total_cents      BIGINT NOT NULL,
    deposit_cents    BIGINT NOT NULL,
    promo_code       TEXT,
    discount_cents   BIGINT NOT NULL DEFAULT 0,
    expires_at       TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_pricing_quote_customer ON pricing_quote (customer_id);
