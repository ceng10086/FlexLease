package com.flexlease.order.domain;

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

    @Column(name = "rent_amount", nullable = false)
    private BigDecimal rentAmount;

    @Column(name = "buyout_amount")
    private BigDecimal buyoutAmount;

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

    protected RentalOrder() {
        // JPA
    }

    private RentalOrder(UUID userId,
                        UUID vendorId,
                        String planType,
                        BigDecimal depositAmount,
                        BigDecimal rentAmount,
                        BigDecimal buyoutAmount,
                        BigDecimal totalAmount,
                        OffsetDateTime leaseStartAt,
                        OffsetDateTime leaseEndAt) {
        this.id = UUID.randomUUID();
        this.orderNo = generateOrderNo();
        this.userId = userId;
        this.vendorId = vendorId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.planType = planType;
        this.depositAmount = depositAmount;
        this.rentAmount = rentAmount;
        this.buyoutAmount = buyoutAmount;
        this.totalAmount = totalAmount;
        this.leaseStartAt = leaseStartAt;
        this.leaseEndAt = leaseEndAt;
        this.extensionCount = 0;
    }

    public static RentalOrder create(UUID userId,
                                     UUID vendorId,
                                     String planType,
                                     BigDecimal depositAmount,
                                     BigDecimal rentAmount,
                                     BigDecimal buyoutAmount,
                                     BigDecimal totalAmount,
                                     OffsetDateTime leaseStartAt,
                                     OffsetDateTime leaseEndAt) {
        return new RentalOrder(userId, vendorId, planType, depositAmount, rentAmount, buyoutAmount, totalAmount, leaseStartAt, leaseEndAt);
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

    public BigDecimal getBuyoutAmount() {
        return buyoutAmount;
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
        this.status = OrderStatus.IN_LEASE;
        this.shippingCarrier = carrier;
        this.shippingTrackingNo = trackingNo;
        if (this.leaseStartAt == null) {
            this.leaseStartAt = OffsetDateTime.now();
        }
    }

    public void confirmReceive() {
        ensureStatus(OrderStatus.IN_LEASE);
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

    public void updateBuyoutAmount(BigDecimal amount) {
        this.buyoutAmount = amount;
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
