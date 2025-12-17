CREATE TABLE IF NOT EXISTS users.credit_adjustment (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    delta INTEGER NOT NULL,
    reason VARCHAR(255),
    operator_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_credit_adjustment_user_id_created_at
    ON users.credit_adjustment (user_id, created_at DESC);
