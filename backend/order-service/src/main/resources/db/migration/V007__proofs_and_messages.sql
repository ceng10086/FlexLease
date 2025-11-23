ALTER TABLE "order".order_event
    ADD COLUMN IF NOT EXISTS actor_role VARCHAR(20);

CREATE TABLE IF NOT EXISTS "order".order_proof (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES "order".rental_order(id) ON DELETE CASCADE,
    proof_type VARCHAR(30) NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    uploaded_by UUID NOT NULL,
    actor_role VARCHAR(20),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_proof_order ON "order".order_proof(order_id, uploaded_at);
