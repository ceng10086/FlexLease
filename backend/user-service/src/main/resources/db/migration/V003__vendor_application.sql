CREATE TABLE IF NOT EXISTS users.vendor_application (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    unified_social_code VARCHAR(50) NOT NULL UNIQUE,
    contact_name VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    contact_email VARCHAR(100),
    province VARCHAR(100),
    city VARCHAR(100),
    address VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    review_remark VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vendor_application_status ON users.vendor_application (status);
CREATE INDEX IF NOT EXISTS idx_vendor_application_owner ON users.vendor_application (owner_user_id);
