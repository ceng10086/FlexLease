ALTER TABLE auth.user_account
    ADD COLUMN IF NOT EXISTS vendor_id UUID;

CREATE INDEX IF NOT EXISTS idx_user_account_vendor ON auth.user_account (vendor_id);
