package com.flexlease.order.domain;

import com.flexlease.common.user.CreditTier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "rental_order", schema = "order")
public class RentalOrder {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_no", nullable = false, unique = true, length = 40)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "plan_type", length = 30)
    private String planType;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "original_deposit_amount", nullable = false)
    private BigDecimal originalDepositAmount;

    @Column(name = "rent_amount", nullable = false)
    private BigDecimal rentAmount;

    @Column(name = "buyout_amount")
    private BigDecimal buyoutAmount;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_tier", nullable = false, length = 30)
    private CreditTier creditTier;

    @Column(name = "deposit_adjustment_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal depositAdjustmentRate;

    @Column(name = "requires_manual_review", nullable = false)
    private boolean requiresManualReview;

    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    @Column(name = "lease_start_at")
    private OffsetDateTime leaseStartAt;

    @Column(name = "lease_end_at")
    private OffsetDateTime leaseEndAt;

    @Column(name = "extension_count", nullable = false)
    private int extensionCount;

    @Column(name = "shipping_carrier", length = 100)
    private String shippingCarrier;

    @Column(name = "shipping_tracking_no", length = 100)
    private String shippingTrackingNo;

    @Column(name = "customer_remark")
    private String customerRemark;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderExtensionRequest> extensionRequests = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderReturnRequest> returnRequests = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProof> proofs = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDispute> disputes = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderSatisfactionSurvey> surveys = new ArrayList<>();

    protected RentalOrder() {
        // JPA
    }

    private RentalOrder(UUID userId,
                        UUID vendorId,
                        String planType,
                        BigDecimal depositAmount,
                        BigDecimal originalDepositAmount,
                        BigDecimal rentAmount,
                        BigDecimal buyoutAmount,
                        BigDecimal totalAmount,
                        Integer creditScore,
                        CreditTier creditTier,
                        BigDecimal depositAdjustmentRate,
                        boolean requiresManualReview,
                        OffsetDateTime leaseStartAt,
                        OffsetDateTime leaseEndAt) {
        this.id = UUID.randomUUID();
        this.orderNo = generateOrderNo();
        this.userId = userId;
        this.vendorId = vendorId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.planType = planType;
        this.depositAmount = depositAmount;
        this.originalDepositAmount = originalDepositAmount;
        this.rentAmount = rentAmount;
        this.buyoutAmount = buyoutAmount;
        this.totalAmount = totalAmount;
        this.creditScore = creditScore;
        this.creditTier = creditTier;
        this.depositAdjustmentRate = depositAdjustmentRate;
        this.requiresManualReview = requiresManualReview;
        this.leaseStartAt = leaseStartAt;
        this.leaseEndAt = leaseEndAt;
        this.extensionCount = 0;
    }

    public static RentalOrder create(UUID userId,
                                     UUID vendorId,
                                     String planType,
                                     BigDecimal depositAmount,
                                     BigDecimal originalDepositAmount,
                                     BigDecimal rentAmount,
                                     BigDecimal buyoutAmount,
                                     BigDecimal totalAmount,
                                     Integer creditScore,
                                     CreditTier creditTier,
                                     BigDecimal depositAdjustmentRate,
                                     boolean requiresManualReview,
                                     OffsetDateTime leaseStartAt,
                                     OffsetDateTime leaseEndAt) {
        return new RentalOrder(userId, vendorId, planType, depositAmount, originalDepositAmount, rentAmount, buyoutAmount, totalAmount,
                creditScore, creditTier, depositAdjustmentRate, requiresManualReview, leaseStartAt, leaseEndAt);
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

    public String getOrderNo() {
        return orderNo;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getPlanType() {
        return planType;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getRentAmount() {
        return rentAmount;
    }

    public BigDecimal getOriginalDepositAmount() {
        return originalDepositAmount;
    }

    public BigDecimal getBuyoutAmount() {
        return buyoutAmount;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public CreditTier getCreditTier() {
        return creditTier;
    }

    public BigDecimal getDepositAdjustmentRate() {
        return depositAdjustmentRate;
    }

    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }

    public UUID getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public OffsetDateTime getLeaseStartAt() {
        return leaseStartAt;
    }

    public OffsetDateTime getLeaseEndAt() {
        return leaseEndAt;
    }

    public int getExtensionCount() {
        return extensionCount;
    }

    public String getShippingCarrier() {
        return shippingCarrier;
    }

    public String getShippingTrackingNo() {
        return shippingTrackingNo;
    }

    public String getCustomerRemark() {
        return customerRemark;
    }

    public void updateCustomerRemark(String remark) {
        if (remark == null) {
            this.customerRemark = null;
            return;
        }
        String normalized = remark.trim();
        this.customerRemark = normalized.isEmpty() ? null : normalized;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<RentalOrderItem> getItems() {
        return items;
    }

    public List<OrderEvent> getEvents() {
        return events;
    }

    public List<OrderExtensionRequest> getExtensionRequests() {
        return extensionRequests;
    }

    public List<OrderReturnRequest> getReturnRequests() {
        return returnRequests;
    }

    public List<OrderProof> getProofs() {
        return proofs;
    }

    public List<OrderDispute> getDisputes() {
        return disputes;
    }

    public List<OrderSatisfactionSurvey> getSurveys() {
        return surveys;
    }

    public void addItem(RentalOrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void addEvent(OrderEvent event) {
        event.setOrder(this);
        events.add(event);
    }

    public void addExtensionRequest(OrderExtensionRequest request) {
        request.setOrder(this);
        extensionRequests.add(request);
    }

    public void addReturnRequest(OrderReturnRequest request) {
        request.setOrder(this);
        returnRequests.add(request);
    }

    public void addProof(OrderProof proof) {
        proof.setOrder(this);
        proofs.add(proof);
    }

    public void addDispute(OrderDispute dispute) {
        dispute.setOrder(this);
        disputes.add(dispute);
    }

    public void addSurvey(OrderSatisfactionSurvey survey) {
        survey.setOrder(this);
        surveys.add(survey);
    }

    public void markPaid() {
        ensureStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.AWAITING_SHIPMENT;
    }

    public void cancel() {
        ensureStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.CANCELLED;
    }

    public void ship(String carrier, String trackingNo) {
        ensureStatus(OrderStatus.AWAITING_SHIPMENT);
        this.status = OrderStatus.AWAITING_RECEIPT;
        this.shippingCarrier = carrier;
        this.shippingTrackingNo = trackingNo;
    }

    public void confirmReceive() {
        ensureStatus(OrderStatus.AWAITING_RECEIPT);
        this.status = OrderStatus.IN_LEASE;
        if (this.leaseStartAt == null) {
            this.leaseStartAt = OffsetDateTime.now();
        }
    }

    public void requestReturn() {
        if (status != OrderStatus.IN_LEASE && status != OrderStatus.RETURN_IN_PROGRESS) {
            throw new IllegalStateException("订单状态不允许退租");
        }
        this.status = OrderStatus.RETURN_REQUESTED;
    }

    public void markReturnInProgress() {
        ensureStatus(OrderStatus.RETURN_REQUESTED);
        this.status = OrderStatus.RETURN_IN_PROGRESS;
    }

    public void completeReturn() {
        if (status != OrderStatus.RETURN_IN_PROGRESS && status != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("订单状态不允许完成退租");
        }
        this.status = OrderStatus.COMPLETED;
        this.leaseEndAt = OffsetDateTime.now();
    }

    public void resumeLease() {
        ensureStatus(OrderStatus.RETURN_REQUESTED);
        this.status = OrderStatus.IN_LEASE;
    }

    public void requestBuyout() {
        if (status != OrderStatus.IN_LEASE) {
            throw new IllegalStateException("仅在租赁中可以申请买断");
        }
        this.status = OrderStatus.BUYOUT_REQUESTED;
    }

    public void confirmBuyout() {
        ensureStatus(OrderStatus.BUYOUT_REQUESTED);
        this.status = OrderStatus.BUYOUT_COMPLETED;
        this.leaseEndAt = OffsetDateTime.now();
    }

    public void rejectBuyout() {
        ensureStatus(OrderStatus.BUYOUT_REQUESTED);
        this.status = OrderStatus.IN_LEASE;
    }

    public void forceClose() {
        if (status == OrderStatus.CANCELLED || status == OrderStatus.COMPLETED || status == OrderStatus.BUYOUT_COMPLETED) {
            throw new IllegalStateException("订单已处于终态，无法强制关闭");
        }
        this.status = OrderStatus.EXCEPTION_CLOSED;
    }

    public void updateBuyoutAmount(BigDecimal amount) {
        this.buyoutAmount = amount;
    }

    public void setPaymentTransactionId(UUID paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    public void increaseExtensionCount(int additionalMonths) {
        this.extensionCount += 1;
        if (leaseEndAt != null) {
            this.leaseEndAt = leaseEndAt.plusMonths(additionalMonths);
        }
    }

    private void ensureStatus(OrderStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException("订单状态不匹配，期望 " + expected + " 实际 " + status);
        }
    }

    private String generateOrderNo() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.CHINA));
        String tail = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        return timestamp + tail;
    }
}
