CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE IF NOT EXISTS products (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sku         VARCHAR(50)     NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    price       NUMERIC(12,2)   NOT NULL CHECK (price >= 0),
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_products_sku UNIQUE (sku)
);

CREATE INDEX IF NOT EXISTS idx_products_status     ON products (status);
CREATE INDEX IF NOT EXISTS idx_products_sku        ON products (sku);
CREATE INDEX IF NOT EXISTS idx_products_name       ON products USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_products_price      ON products (price);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products (created_at);

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_products_updated_at ON products;
CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
