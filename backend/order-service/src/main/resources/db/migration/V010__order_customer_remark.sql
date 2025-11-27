ALTER TABLE "order".rental_order
    ADD COLUMN IF NOT EXISTS customer_remark TEXT;
