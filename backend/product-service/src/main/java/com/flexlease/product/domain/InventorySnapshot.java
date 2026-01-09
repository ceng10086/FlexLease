package com.flexlease.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 库存变更流水（快照）。
 * <p>
 * 每次对 SKU 的库存进行入库/出库/预占/释放等操作时，都会记录一条流水，便于审计与问题排查。
 */
@Entity
@Table(name = "inventory_snapshot", schema = "product")
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private InventoryChangeType changeType;

    @Column(name = "change_qty", nullable = false)
    private int changeQty;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected InventorySnapshot() {
        // JPA 需要无参构造
    }

    private InventorySnapshot(ProductSku sku,
                              InventoryChangeType changeType,
                              int changeQty,
                              int balanceAfter,
                              UUID referenceId) {
        this.sku = sku;
        this.changeType = changeType;
        this.changeQty = changeQty;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
    }

    public static InventorySnapshot record(ProductSku sku,
                                           InventoryChangeType changeType,
                                           int changeQty,
                                           int balanceAfter,
                                           UUID referenceId) {
        return new InventorySnapshot(sku, changeType, changeQty, balanceAfter, referenceId);
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ProductSku getSku() {
        return sku;
    }

    public InventoryChangeType getChangeType() {
        return changeType;
    }

    public int getChangeQty() {
        return changeQty;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
