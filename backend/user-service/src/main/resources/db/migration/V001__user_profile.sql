CREATE TABLE IF NOT EXISTS users.user_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    full_name VARCHAR(100),
    gender VARCHAR(10),
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_profile_user ON users.user_profile (user_id);
