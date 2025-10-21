package com.flexlease.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_sku", schema = "product")
public class ProductSku {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_plan_id")
    private RentalPlan rentalPlan;

    @Column(name = "sku_code", unique = true, length = 64)
    private String skuCode;

    @Column(name = "attributes")
    private String attributes;

    @Column(name = "stock_total", nullable = false)
    private int stockTotal;

    @Column(name = "stock_available", nullable = false)
    private int stockAvailable;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProductSkuStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProductSku() {
        // JPA
    }

    private ProductSku(Product product,
                       RentalPlan rentalPlan,
                       String skuCode,
                       String attributes,
                       int stockTotal,
                       int stockAvailable) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.rentalPlan = rentalPlan;
        this.skuCode = skuCode;
        this.attributes = attributes;
        this.stockTotal = stockTotal;
        this.stockAvailable = stockAvailable;
        this.status = ProductSkuStatus.ACTIVE;
    }

    public static ProductSku create(Product product,
                                     RentalPlan rentalPlan,
                                     String skuCode,
                                     String attributes,
                                     int stockTotal) {
        return new ProductSku(product, rentalPlan, skuCode, attributes, stockTotal, stockTotal);
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public RentalPlan getRentalPlan() {
        return rentalPlan;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getAttributes() {
        return attributes;
    }

    public int getStockTotal() {
        return stockTotal;
    }

    public int getStockAvailable() {
        return stockAvailable;
    }

    public ProductSkuStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateBasicInfo(String skuCode,
                                String attributes,
                                int stockTotal,
                                int stockAvailable,
                                ProductSkuStatus status) {
        this.skuCode = skuCode;
        this.attributes = attributes;
        if (stockAvailable > stockTotal) {
            throw new IllegalArgumentException("可用库存不能大于库存总量");
        }
        this.stockTotal = stockTotal;
        this.stockAvailable = stockAvailable;
        this.status = status;
    }

    public void setRentalPlan(RentalPlan rentalPlan) {
        this.rentalPlan = rentalPlan;
    }

    public void inbound(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("入库数量需大于 0");
        }
        stockTotal += quantity;
        stockAvailable += quantity;
    }

    public void outbound(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("出库数量需大于 0");
        }
        if (stockAvailable < quantity || stockTotal < quantity) {
            throw new IllegalArgumentException("可用库存不足");
        }
        stockAvailable -= quantity;
        stockTotal -= quantity;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("预占数量需大于 0");
        }
        if (stockAvailable < quantity) {
            throw new IllegalArgumentException("可用库存不足");
        }
        stockAvailable -= quantity;
    }

    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("释放数量需大于 0");
        }
        stockAvailable = Math.min(stockAvailable + quantity, stockTotal);
    }

    public void setStatus(ProductSkuStatus status) {
        this.status = status;
    }
}
