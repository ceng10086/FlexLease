CREATE TABLE IF NOT EXISTS "order".dispute_ai_suggestion (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES "order".rental_order (id) ON DELETE CASCADE,
    dispute_id UUID NOT NULL UNIQUE,
    model VARCHAR(100),
    prompt_version VARCHAR(40),
    tone VARCHAR(20),
    input_hash VARCHAR(64),
    output_json TEXT NOT NULL,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dispute_ai_suggestion_order_id ON "order".dispute_ai_suggestion(order_id);
