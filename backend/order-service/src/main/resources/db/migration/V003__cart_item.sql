CREATE TABLE IF NOT EXISTS "order".cart_item (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    plan_id UUID NULL,
    product_name VARCHAR(200) NOT NULL,
    sku_code VARCHAR(64) NULL,
    plan_snapshot TEXT NULL,
    quantity INTEGER NOT NULL,
    unit_rent_amount NUMERIC(18,2) NOT NULL,
    unit_deposit_amount NUMERIC(18,2) NOT NULL,
    buyout_price NUMERIC(18,2) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cart_item_user ON "order".cart_item(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_user_sku ON "order".cart_item(user_id, sku_id);
