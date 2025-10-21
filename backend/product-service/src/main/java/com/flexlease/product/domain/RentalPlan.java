package com.flexlease.product.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rental_plan", schema = "product")
public class RentalPlan {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 30)
    private RentalPlanType planType;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "deposit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "rent_amount_monthly", nullable = false, precision = 18, scale = 2)
    private BigDecimal rentAmountMonthly;

    @Column(name = "buyout_price", precision = 18, scale = 2)
    private BigDecimal buyoutPrice;

    @Column(name = "allow_extend")
    private boolean allowExtend;

    @Column(name = "extension_unit", length = 10)
    private String extensionUnit;

    @Column(name = "extension_price", precision = 18, scale = 2)
    private BigDecimal extensionPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RentalPlanStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "rentalPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSku> skus = new ArrayList<>();

    protected RentalPlan() {
        // JPA
    }

    private RentalPlan(Product product,
                       RentalPlanType planType,
                       int termMonths,
                       BigDecimal depositAmount,
                       BigDecimal rentAmountMonthly,
                       BigDecimal buyoutPrice,
                       boolean allowExtend,
                       String extensionUnit,
                       BigDecimal extensionPrice) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.planType = planType;
        this.termMonths = termMonths;
        this.depositAmount = depositAmount;
        this.rentAmountMonthly = rentAmountMonthly;
        this.buyoutPrice = buyoutPrice;
        this.allowExtend = allowExtend;
        this.extensionUnit = extensionUnit;
        this.extensionPrice = extensionPrice;
        this.status = RentalPlanStatus.DRAFT;
    }

    public static RentalPlan create(Product product,
                                     RentalPlanType planType,
                                     int termMonths,
                                     BigDecimal depositAmount,
                                     BigDecimal rentAmountMonthly,
                                     BigDecimal buyoutPrice,
                                     boolean allowExtend,
                                     String extensionUnit,
                                     BigDecimal extensionPrice) {
        return new RentalPlan(product, planType, termMonths, depositAmount, rentAmountMonthly,
                buyoutPrice, allowExtend, extensionUnit, extensionPrice);
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

    public RentalPlanType getPlanType() {
        return planType;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getRentAmountMonthly() {
        return rentAmountMonthly;
    }

    public BigDecimal getBuyoutPrice() {
        return buyoutPrice;
    }

    public boolean isAllowExtend() {
        return allowExtend;
    }

    public String getExtensionUnit() {
        return extensionUnit;
    }

    public BigDecimal getExtensionPrice() {
        return extensionPrice;
    }

    public RentalPlanStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ProductSku> getSkus() {
        return skus;
    }

    public void updateBasicInfo(RentalPlanType planType,
                                int termMonths,
                                BigDecimal depositAmount,
                                BigDecimal rentAmountMonthly,
                                BigDecimal buyoutPrice,
                                boolean allowExtend,
                                String extensionUnit,
                                BigDecimal extensionPrice) {
        this.planType = planType;
        this.termMonths = termMonths;
        this.depositAmount = depositAmount;
        this.rentAmountMonthly = rentAmountMonthly;
        this.buyoutPrice = buyoutPrice;
        this.allowExtend = allowExtend;
        this.extensionUnit = extensionUnit;
        this.extensionPrice = extensionPrice;
    }

    public void activate() {
        this.status = RentalPlanStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = RentalPlanStatus.INACTIVE;
    }
}
