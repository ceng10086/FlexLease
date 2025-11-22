ALTER TABLE "order".rental_order
    ADD COLUMN IF NOT EXISTS original_deposit_amount NUMERIC(19,2) NOT NULL DEFAULT 0;

ALTER TABLE "order".rental_order
    ADD COLUMN IF NOT EXISTS credit_score INTEGER NOT NULL DEFAULT 60;

ALTER TABLE "order".rental_order
    ADD COLUMN IF NOT EXISTS credit_tier VARCHAR(30) NOT NULL DEFAULT 'STANDARD';

ALTER TABLE "order".rental_order
    ADD COLUMN IF NOT EXISTS deposit_adjustment_rate NUMERIC(5,2) NOT NULL DEFAULT 1.00;

ALTER TABLE "order".rental_order
    ADD COLUMN IF NOT EXISTS requires_manual_review BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE "order".rental_order
SET original_deposit_amount = deposit_amount
WHERE original_deposit_amount = 0;
