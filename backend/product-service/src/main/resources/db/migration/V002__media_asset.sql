CREATE TABLE IF NOT EXISTS product.media_asset (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product.product(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_media_asset_product ON product.media_asset(product_id, sort_order);
