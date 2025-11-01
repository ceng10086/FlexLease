CREATE TABLE IF NOT EXISTS users.vendor (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    contact_email VARCHAR(100),
    province VARCHAR(100),
    city VARCHAR(100),
    address VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vendor_owner ON users.vendor (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_vendor_status ON users.vendor (status);
