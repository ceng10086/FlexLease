CREATE TABLE IF NOT EXISTS auth.role (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS auth.user_account (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS auth.user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES auth.user_account(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES auth.role(id)
);

CREATE INDEX IF NOT EXISTS idx_user_account_username ON auth.user_account (username);
