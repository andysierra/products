CREATE TABLE IF NOT EXISTS inventory (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID            NOT NULL,
    available   INTEGER         NOT NULL DEFAULT 0 CHECK (available >= 0),
    reserved    INTEGER         NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    version     BIGINT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_product UNIQUE (product_id)
);

CREATE TABLE IF NOT EXISTS purchases (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID            NOT NULL,
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    status          VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED',
    idempotency_key VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_purchases_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_purchases_product_id ON purchases (product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory (product_id);

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_inventory_updated_at') THEN
        CREATE TRIGGER trg_inventory_updated_at
            BEFORE UPDATE ON inventory
            FOR EACH ROW EXECUTE FUNCTION update_updated_at();
    END IF;
END $$;
