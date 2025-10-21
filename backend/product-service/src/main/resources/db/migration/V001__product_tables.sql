CREATE TABLE IF NOT EXISTS product.product (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    category_code VARCHAR(100) NOT NULL,
    description TEXT,
    cover_image_url VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    review_remark TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS product.rental_plan (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product.product(id) ON DELETE CASCADE,
    plan_type VARCHAR(30) NOT NULL,
    term_months INT NOT NULL,
    deposit_amount NUMERIC(18,2) NOT NULL,
    rent_amount_monthly NUMERIC(18,2) NOT NULL,
    buyout_price NUMERIC(18,2),
    allow_extend BOOLEAN DEFAULT TRUE,
    extension_unit VARCHAR(10),
    extension_price NUMERIC(18,2),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS product.product_sku (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product.product(id) ON DELETE CASCADE,
    rental_plan_id UUID REFERENCES product.rental_plan(id) ON DELETE SET NULL,
    sku_code VARCHAR(64) UNIQUE,
    attributes TEXT,
    stock_total INT NOT NULL,
    stock_available INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS product.inventory_snapshot (
    id BIGSERIAL PRIMARY KEY,
    sku_id UUID NOT NULL REFERENCES product.product_sku(id) ON DELETE CASCADE,
    change_type VARCHAR(30) NOT NULL,
    change_qty INT NOT NULL,
    balance_after INT NOT NULL,
    reference_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_vendor ON product.product(vendor_id);
CREATE INDEX IF NOT EXISTS idx_product_status ON product.product(status);
CREATE INDEX IF NOT EXISTS idx_rental_plan_product ON product.rental_plan(product_id);
CREATE INDEX IF NOT EXISTS idx_product_sku_plan ON product.product_sku(rental_plan_id);
CREATE INDEX IF NOT EXISTS idx_inventory_snapshot_sku ON product.inventory_snapshot(sku_id);
