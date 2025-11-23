ALTER TABLE "payment".payment_transaction
    ADD COLUMN IF NOT EXISTS commission_rate NUMERIC(5,4);
