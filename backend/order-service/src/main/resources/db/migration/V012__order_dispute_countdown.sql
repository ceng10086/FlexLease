ALTER TABLE "order".order_dispute
    ADD COLUMN IF NOT EXISTS countdown_notified_at TIMESTAMP WITH TIME ZONE;
