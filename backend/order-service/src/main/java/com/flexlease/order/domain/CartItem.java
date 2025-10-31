package com.flexlease.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cart_item", schema = "\"order\"")
public class CartItem {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "sku_code", length = 64)
    private String skuCode;

    @Column(name = "plan_snapshot")
    private String planSnapshot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_rent_amount", nullable = false)
    private BigDecimal unitRentAmount;

    @Column(name = "unit_deposit_amount", nullable = false)
    private BigDecimal unitDepositAmount;

    @Column(name = "buyout_price")
    private BigDecimal buyoutPrice;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CartItem() {
        // JPA
    }

    private CartItem(UUID userId,
                     UUID vendorId,
                     UUID productId,
                     UUID skuId,
                     UUID planId,
                     String productName,
                     String skuCode,
                     String planSnapshot,
                     int quantity,
                     BigDecimal unitRentAmount,
                     BigDecimal unitDepositAmount,
                     BigDecimal buyoutPrice) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.vendorId = vendorId;
        this.productId = productId;
        this.skuId = skuId;
        this.planId = planId;
        this.productName = productName;
        this.skuCode = skuCode;
        this.planSnapshot = planSnapshot;
        this.quantity = quantity;
        this.unitRentAmount = unitRentAmount;
        this.unitDepositAmount = unitDepositAmount;
        this.buyoutPrice = buyoutPrice;
    }

    public static CartItem create(UUID userId,
                                  UUID vendorId,
                                  UUID productId,
                                  UUID skuId,
                                  UUID planId,
                                  String productName,
                                  String skuCode,
                                  String planSnapshot,
                                  int quantity,
                                  BigDecimal unitRentAmount,
                                  BigDecimal unitDepositAmount,
                                  BigDecimal buyoutPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("购物车数量必须大于 0");
        }
        return new CartItem(userId, vendorId, productId, skuId, planId, productName, skuCode, planSnapshot,
                quantity, unitRentAmount, unitDepositAmount, buyoutPrice);
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getPlanSnapshot() {
        return planSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitRentAmount() {
        return unitRentAmount;
    }

    public BigDecimal getUnitDepositAmount() {
        return unitDepositAmount;
    }

    public BigDecimal getBuyoutPrice() {
        return buyoutPrice;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void increaseQuantity(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("数量增量必须大于 0");
        }
        this.quantity += delta;
    }

    public void updateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于 0");
        }
        this.quantity = quantity;
    }

    public void refreshDetails(UUID vendorId,
                               UUID productId,
                               UUID planId,
                               String productName,
                               String skuCode,
                               String planSnapshot,
                               BigDecimal unitRentAmount,
                               BigDecimal unitDepositAmount,
                               BigDecimal buyoutPrice) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.planId = planId;
        this.productName = productName;
        this.skuCode = skuCode;
        this.planSnapshot = planSnapshot;
        this.unitRentAmount = unitRentAmount;
        this.unitDepositAmount = unitDepositAmount;
        this.buyoutPrice = buyoutPrice;
    }
}
