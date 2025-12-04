ALTER TABLE "order".order_dispute
    ADD COLUMN IF NOT EXISTS initiator_phone_memo VARCHAR(1000);

ALTER TABLE "order".order_dispute
    ADD COLUMN IF NOT EXISTS respondent_phone_memo VARCHAR(1000);

ALTER TABLE "order".order_dispute
    ADD COLUMN IF NOT EXISTS initiator_attachment_ids TEXT;

ALTER TABLE "order".order_dispute
    ADD COLUMN IF NOT EXISTS respondent_attachment_ids TEXT;
