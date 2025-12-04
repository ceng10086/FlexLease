CREATE TABLE IF NOT EXISTS product.product_inquiry (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    requester_id UUID,
    contact_name VARCHAR(100),
    contact_method VARCHAR(120),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reply TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE product.product_inquiry
    ADD CONSTRAINT fk_product_inquiry_product
        FOREIGN KEY (product_id) REFERENCES product.product (id)
            ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_product_inquiry_vendor_status
    ON product.product_inquiry (vendor_id, status);

CREATE INDEX IF NOT EXISTS idx_product_inquiry_product
    ON product.product_inquiry (product_id);
