package com.flexlease.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rental_order_item", schema = "order")
public class RentalOrderItem {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_id")
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

    protected RentalOrderItem() {
        // JPA 需要无参构造
    }

    private RentalOrderItem(UUID productId,
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

    public static RentalOrderItem create(UUID productId,
                                         UUID skuId,
                                         UUID planId,
                                         String productName,
                                         String skuCode,
                                         String planSnapshot,
                                         int quantity,
                                         BigDecimal unitRentAmount,
                                         BigDecimal unitDepositAmount,
                                         BigDecimal buyoutPrice) {
        return new RentalOrderItem(productId, skuId, planId, productName, skuCode, planSnapshot, quantity, unitRentAmount, unitDepositAmount, buyoutPrice);
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

    public RentalOrder getOrder() {
        return order;
    }

    public void setOrder(RentalOrder order) {
        this.order = order;
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
}
