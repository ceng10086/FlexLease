ALTER TABLE users.user_profile
    ADD COLUMN IF NOT EXISTS kyc_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users.user_profile
    ADD COLUMN IF NOT EXISTS kyc_verified_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE users.user_profile
    ADD COLUMN IF NOT EXISTS payment_streak INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users.user_profile
    ADD COLUMN IF NOT EXISTS payment_streak_milestone INTEGER NOT NULL DEFAULT 0;
