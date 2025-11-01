CREATE TABLE IF NOT EXISTS "order".rental_contract (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE REFERENCES "order".rental_order(id) ON DELETE CASCADE,
    contract_number VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    signature VARCHAR(255),
    signed_by UUID,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rental_contract_order ON "order".rental_contract(order_id);
CREATE INDEX IF NOT EXISTS idx_rental_contract_status ON "order".rental_contract(status);
