CREATE TABLE IF NOT EXISTS "order".rental_order (
    id UUID PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    plan_type VARCHAR(30),
    total_amount NUMERIC(18,2) NOT NULL,
    deposit_amount NUMERIC(18,2) NOT NULL,
    rent_amount NUMERIC(18,2) NOT NULL,
    buyout_amount NUMERIC(18,2),
    lease_start_at TIMESTAMP WITH TIME ZONE,
    lease_end_at TIMESTAMP WITH TIME ZONE,
    extension_count INT NOT NULL,
    shipping_carrier VARCHAR(100),
    shipping_tracking_no VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS "order".rental_order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES "order".rental_order(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    sku_id UUID,
    plan_id UUID,
    product_name VARCHAR(200) NOT NULL,
    sku_code VARCHAR(64),
    plan_snapshot VARCHAR(255),
    quantity INT NOT NULL,
    unit_rent_amount NUMERIC(18,2) NOT NULL,
    unit_deposit_amount NUMERIC(18,2) NOT NULL,
    buyout_price NUMERIC(18,2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS "order".order_event (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES "order".rental_order(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    description TEXT,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS "order".order_extension_request (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES "order".rental_order(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    additional_months INT NOT NULL,
    requested_by UUID NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decision_by UUID,
    decision_at TIMESTAMP WITH TIME ZONE,
    remark TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS "order".order_return_request (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES "order".rental_order(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    reason TEXT,
    logistics_company VARCHAR(100),
    tracking_number VARCHAR(100),
    requested_by UUID NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decision_by UUID,
    decision_at TIMESTAMP WITH TIME ZONE,
    remark TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rental_order_user ON "order".rental_order(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_rental_order_vendor ON "order".rental_order(vendor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_rental_order_status ON "order".rental_order(status);
CREATE INDEX IF NOT EXISTS idx_order_event_order ON "order".order_event(order_id);
CREATE INDEX IF NOT EXISTS idx_extension_order ON "order".order_extension_request(order_id, status);
CREATE INDEX IF NOT EXISTS idx_return_order ON "order".order_return_request(order_id, status);
