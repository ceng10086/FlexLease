ALTER TABLE "order".rental_order
    ADD COLUMN IF NOT EXISTS payment_transaction_id UUID;
