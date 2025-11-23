package com.flexlease.payment.domain;

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
@Table(name = "payment_transaction", schema = "payment")
public class PaymentTransaction {

    private static final DateTimeFormatter NUMBER_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.CHINA);

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "transaction_no", nullable = false, unique = true, length = 60)
    private String transactionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene", nullable = false, length = 20)
    private PaymentScene scene;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private PaymentChannel channel;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    @Column(name = "channel_transaction_no", length = 100)
    private String channelTransactionNo;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "commission_rate")
    private BigDecimal commissionRate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentSplit> splits = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundTransaction> refunds = new ArrayList<>();

    protected PaymentTransaction() {
        // JPA
    }

    private PaymentTransaction(UUID orderId,
                               UUID userId,
                               UUID vendorId,
                               PaymentScene scene,
                               PaymentChannel channel,
                               BigDecimal amount,
                               String description) {
        this.id = UUID.randomUUID();
        this.transactionNo = generateTransactionNo();
        this.orderId = orderId;
        this.userId = userId;
        this.vendorId = vendorId;
        this.scene = scene;
        this.channel = channel;
        this.amount = amount;
        this.description = description;
        this.status = PaymentStatus.PENDING;
    }

    public static PaymentTransaction create(UUID orderId,
                                            UUID userId,
                                            UUID vendorId,
                                            PaymentScene scene,
                                            PaymentChannel channel,
                                            BigDecimal amount,
                                            String description) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("支付金额必须大于 0");
        }
        return new PaymentTransaction(orderId, userId, vendorId, scene, channel, amount, description);
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

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public PaymentScene getScene() {
        return scene;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentChannel getChannel() {
        return channel;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getChannelTransactionNo() {
        return channelTransactionNo;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<PaymentSplit> getSplits() {
        return splits;
    }

    public List<RefundTransaction> getRefunds() {
        return refunds;
    }

    public void addSplit(PaymentSplit split) {
        if (split == null) {
            throw new IllegalArgumentException("分账信息不能为空");
        }
        split.setTransaction(this);
        splits.add(split);
    }

    public void markSucceeded(String channelTransactionNo, OffsetDateTime paidAt) {
        ensurePending();
        this.status = PaymentStatus.SUCCEEDED;
        this.channelTransactionNo = channelTransactionNo;
        this.paidAt = paidAt != null ? paidAt : OffsetDateTime.now();
    }

    public void markFailed(String channelTransactionNo) {
        ensurePending();
        this.status = PaymentStatus.FAILED;
        this.channelTransactionNo = channelTransactionNo;
    }

    public RefundTransaction createRefund(BigDecimal refundAmount, String reason) {
        if (status != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("仅成功的支付可以退款");
        }
        if (refundAmount == null || refundAmount.signum() <= 0) {
            throw new IllegalArgumentException("退款金额必须大于 0");
        }
        if (refundAmount.compareTo(getRemainingRefundableAmount()) > 0) {
            throw new IllegalStateException("退款金额超出可退额度");
        }
        RefundTransaction refund = RefundTransaction.create(refundAmount, reason);
        refund.setTransaction(this);
        refunds.add(refund);
        return refund;
    }

    public BigDecimal getRemainingRefundableAmount() {
        BigDecimal refunded = refunds.stream()
                .filter(ref -> ref.getStatus() != RefundStatus.FAILED)
                .map(RefundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = amount.subtract(refunded);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    private void ensurePending() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("支付流水不是待处理状态");
        }
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    private String generateTransactionNo() {
        String timestamp = OffsetDateTime.now().format(NUMBER_FORMATTER);
        String tail = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "P" + timestamp + tail;
    }
}
