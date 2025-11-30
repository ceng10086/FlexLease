ALTER TABLE "order".cart_item
    ADD CONSTRAINT uq_cart_item_user_sku UNIQUE (user_id, sku_id);
