CREATE TABLE IF NOT EXISTS "payment".payment_transaction (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    transaction_no VARCHAR(60) NOT NULL UNIQUE,
    scene VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    description TEXT,
    channel_transaction_no VARCHAR(100),
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS "payment".payment_split (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES "payment".payment_transaction(id) ON DELETE CASCADE,
    split_type VARCHAR(30) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    beneficiary VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS "payment".refund_transaction (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES "payment".payment_transaction(id) ON DELETE CASCADE,
    refund_no VARCHAR(60) NOT NULL UNIQUE,
    amount NUMERIC(18,2) NOT NULL,
    reason VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    refunded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payment_tx_order_scene ON "payment".payment_transaction(order_id, scene);
CREATE INDEX IF NOT EXISTS idx_payment_tx_vendor_status ON "payment".payment_transaction(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_payment_tx_paid_at ON "payment".payment_transaction(paid_at);
CREATE INDEX IF NOT EXISTS idx_refund_tx_transaction ON "payment".refund_transaction(transaction_id);
