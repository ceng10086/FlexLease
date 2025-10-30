package com.flexlease.order.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.order.client.PaymentClient;
import com.flexlease.order.client.PaymentStatus;
import com.flexlease.order.client.PaymentTransactionView;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.domain.ExtensionRequestStatus;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderExtensionRequest;
import com.flexlease.order.domain.OrderReturnRequest;
import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.domain.RentalOrderItem;
import com.flexlease.order.domain.ReturnRequestStatus;
import com.flexlease.order.dto.CreateOrderRequest;
import com.flexlease.order.dto.OrderActorRequest;
import com.flexlease.order.dto.OrderBuyoutApplyRequest;
import com.flexlease.order.dto.OrderBuyoutDecisionRequest;
import com.flexlease.order.dto.OrderCancelRequest;
import com.flexlease.order.dto.OrderExtensionApplyRequest;
import com.flexlease.order.dto.OrderExtensionDecisionRequest;
import com.flexlease.order.dto.OrderItemRequest;
import com.flexlease.order.dto.OrderPaymentRequest;
import com.flexlease.order.dto.OrderPreviewRequest;
import com.flexlease.order.dto.OrderPreviewResponse;
import com.flexlease.order.dto.OrderReturnApplyRequest;
import com.flexlease.order.dto.OrderReturnDecisionRequest;
import com.flexlease.order.dto.OrderShipmentRequest;
import com.flexlease.order.dto.PagedResponse;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.dto.RentalOrderSummaryResponse;
import com.flexlease.order.repository.OrderExtensionRequestRepository;
import com.flexlease.order.repository.OrderReturnRequestRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class RentalOrderService {

    private final RentalOrderRepository rentalOrderRepository;
    private final OrderExtensionRequestRepository extensionRequestRepository;
    private final OrderReturnRequestRepository returnRequestRepository;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;
    private final OrderAssembler assembler;

    public RentalOrderService(RentalOrderRepository rentalOrderRepository,
                              OrderExtensionRequestRepository extensionRequestRepository,
                              OrderReturnRequestRepository returnRequestRepository,
                              PaymentClient paymentClient,
                              NotificationClient notificationClient,
                              OrderAssembler assembler) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.extensionRequestRepository = extensionRequestRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.paymentClient = paymentClient;
        this.notificationClient = notificationClient;
        this.assembler = assembler;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderPreviewResponse previewOrder(OrderPreviewRequest request) {
        Totals totals = calculateTotals(request.items());
        return new OrderPreviewResponse(totals.depositAmount, totals.rentAmount, totals.totalAmount);
    }

    public RentalOrderResponse createOrder(CreateOrderRequest request) {
        Totals totals = calculateTotals(request.items());

        RentalOrder order = RentalOrder.create(
                request.userId(),
                request.vendorId(),
                request.planType(),
                totals.depositAmount,
                totals.rentAmount,
                totals.buyoutAmount,
                totals.totalAmount,
                request.leaseStartAt(),
                request.leaseEndAt()
        );

        for (OrderItemRequest itemRequest : request.items()) {
            RentalOrderItem item = RentalOrderItem.create(
                    itemRequest.productId(),
                    itemRequest.skuId(),
                    itemRequest.planId(),
                    itemRequest.productName(),
                    itemRequest.skuCode(),
                    itemRequest.planSnapshot(),
                    itemRequest.quantity(),
                    itemRequest.unitRentAmount(),
                    itemRequest.unitDepositAmount(),
                    itemRequest.buyoutPrice()
            );
            order.addItem(item);
        }

        order.addEvent(OrderEvent.record(OrderEventType.ORDER_CREATED, "订单创建", request.userId()));
        RentalOrder saved = rentalOrderRepository.save(order);
        return assembler.toOrderResponse(saved);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public RentalOrderResponse getOrder(UUID orderId) {
        RentalOrder order = rentalOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
        return assembler.toOrderResponse(order);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResponse<RentalOrderSummaryResponse> listOrdersForUser(UUID userId, OrderStatus status, Pageable pageable) {
        Page<RentalOrder> page = status == null
                ? rentalOrderRepository.findByUserId(userId, pageable)
                : rentalOrderRepository.findByUserIdAndStatus(userId, status, pageable);
        return toPagedResponse(page);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResponse<RentalOrderSummaryResponse> listOrdersForVendor(UUID vendorId, OrderStatus status, Pageable pageable) {
        Page<RentalOrder> page = status == null
                ? rentalOrderRepository.findByVendorId(vendorId, pageable)
                : rentalOrderRepository.findByVendorIdAndStatus(vendorId, status, pageable);
        return toPagedResponse(page);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResponse<RentalOrderSummaryResponse> listOrdersForAdmin(UUID userId, UUID vendorId, OrderStatus status, Pageable pageable) {
        if (userId != null && vendorId != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "userId 与 vendorId 不能同时提供");
        }
        Page<RentalOrder> page;
        if (userId != null) {
            page = status == null
                    ? rentalOrderRepository.findByUserId(userId, pageable)
                    : rentalOrderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else if (vendorId != null) {
            page = status == null
                    ? rentalOrderRepository.findByVendorId(vendorId, pageable)
                    : rentalOrderRepository.findByVendorIdAndStatus(vendorId, status, pageable);
        } else if (status != null) {
            page = rentalOrderRepository.findByStatus(status, pageable);
        } else {
            page = rentalOrderRepository.findAll(pageable);
        }
        return toPagedResponse(page);
    }

    public RentalOrderResponse confirmPayment(UUID orderId, OrderPaymentRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureUser(order, request.userId());
        UUID transactionId = parseTransactionId(request.paymentReference());
        PaymentTransactionView transaction = paymentClient.loadTransaction(transactionId);
        ensurePaymentMatches(order, transaction, request);
        order.markPaid();
        order.addEvent(OrderEvent.record(OrderEventType.PAYMENT_CONFIRMED,
            buildPaymentMessage(transaction),
                request.userId()));
        notifyUser(order, "订单支付成功", "订单 %s 支付成功，实付 ¥%s。".formatted(order.getOrderNo(), request.paidAmount()));
        notifyVendor(order, "订单待发货", "订单 %s 已完成支付，请尽快安排发货。".formatted(order.getOrderNo()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse cancelOrder(UUID orderId, OrderCancelRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureUser(order, request.userId());
        order.cancel();
        order.addEvent(OrderEvent.record(OrderEventType.ORDER_CANCELLED,
                request.reason() == null ? "用户取消订单" : request.reason(),
                request.userId()));
        notifyVendor(order, "订单已取消", "订单 %s 已被用户取消。".formatted(order.getOrderNo()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse shipOrder(UUID orderId, OrderShipmentRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureVendor(order, request.vendorId());
        order.ship(request.carrier(), request.trackingNumber());
        order.addEvent(OrderEvent.record(OrderEventType.ORDER_SHIPPED,
                "发货信息: " + request.carrier() + " / " + request.trackingNumber(),
                request.vendorId()));
        notifyUser(order, "订单已发货", "订单 %s 已发货，承运方 %s，运单号 %s。"
            .formatted(order.getOrderNo(), request.carrier(), request.trackingNumber()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse confirmReceive(UUID orderId, OrderActorRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureUser(order, request.actorId());
        order.confirmReceive();
        order.addEvent(OrderEvent.record(OrderEventType.ORDER_RECEIVED, "用户确认收货", request.actorId()));
        notifyVendor(order, "买家确认收货", "订单 %s 已确认收货，租期正式开始计算。".formatted(order.getOrderNo()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse applyExtension(UUID orderId, OrderExtensionApplyRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureUser(order, request.userId());
        if (order.getStatus() != OrderStatus.IN_LEASE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "当前状态不支持续租");
        }
        extensionRequestRepository.findFirstByOrderIdAndStatusOrderByRequestedAtDesc(orderId, ExtensionRequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "已有待处理的续租申请");
                });
        OrderExtensionRequest extensionRequest = OrderExtensionRequest.create(request.additionalMonths(), request.userId(), request.remark());
        order.addExtensionRequest(extensionRequest);
        order.addEvent(OrderEvent.record(OrderEventType.EXTENSION_REQUESTED,
                "续租申请: " + request.additionalMonths() + " 个月",
                request.userId()));
        notifyVendor(order, "收到续租申请", "订单 %s 用户申请续租 %d 个月。"
            .formatted(order.getOrderNo(), request.additionalMonths()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse decideExtension(UUID orderId, OrderExtensionDecisionRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureVendor(order, request.vendorId());
        OrderExtensionRequest extensionRequest = extensionRequestRepository
                .findFirstByOrderIdAndStatusOrderByRequestedAtDesc(orderId, ExtensionRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "没有待处理的续租申请"));
        if (request.approve()) {
            extensionRequest.approve(request.vendorId(), request.remark());
            order.increaseExtensionCount(extensionRequest.getAdditionalMonths());
            order.addEvent(OrderEvent.record(OrderEventType.EXTENSION_APPROVED,
                    "续租通过: " + extensionRequest.getAdditionalMonths() + " 个月",
                    request.vendorId()));
            notifyUser(order, "续租申请通过", "订单 %s 续租成功，追加 %d 个月租期。"
                .formatted(order.getOrderNo(), extensionRequest.getAdditionalMonths()));
        } else {
            extensionRequest.reject(request.vendorId(), request.remark());
            order.addEvent(OrderEvent.record(OrderEventType.EXTENSION_REJECTED,
                    request.remark() == null ? "续租申请被拒绝" : request.remark(),
                    request.vendorId()));
            notifyUser(order, "续租申请被拒", "订单 %s 的续租申请未通过，原因：%s"
                .formatted(order.getOrderNo(), request.remark() == null ? "无" : request.remark()));
        }
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse applyReturn(UUID orderId, OrderReturnApplyRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureUser(order, request.userId());
        if (order.getStatus() != OrderStatus.IN_LEASE && order.getStatus() != OrderStatus.RETURN_IN_PROGRESS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "当前状态不支持退租");
        }
        returnRequestRepository.findFirstByOrderIdAndStatusOrderByRequestedAtDesc(orderId, ReturnRequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "已有待处理的退租申请");
                });
        order.requestReturn();
        OrderReturnRequest returnRequest = OrderReturnRequest.create(
                request.reason(),
                request.logisticsCompany(),
                request.trackingNumber(),
                request.userId()
        );
        order.addReturnRequest(returnRequest);
        order.addEvent(OrderEvent.record(OrderEventType.RETURN_REQUESTED,
                request.reason() == null ? "发起退租" : request.reason(),
                request.userId()));
        notifyVendor(order, "收到退租申请", "订单 %s 用户提交退租申请，请及时处理。"
            .formatted(order.getOrderNo()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse decideReturn(UUID orderId, OrderReturnDecisionRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureVendor(order, request.vendorId());
        OrderReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndStatusOrderByRequestedAtDesc(orderId, ReturnRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "没有待处理的退租申请"));
        if (request.approve()) {
            returnRequest.approve(request.vendorId(), request.remark());
            order.completeReturn();
            order.addEvent(OrderEvent.record(OrderEventType.RETURN_APPROVED,
                    request.remark() == null ? "退租完成" : request.remark(),
                    request.vendorId()));
            notifyUser(order, "退租已完成", "订单 %s 的退租申请已通过。".formatted(order.getOrderNo()));
        } else {
            returnRequest.reject(request.vendorId(), request.remark());
            order.resumeLease();
            order.addEvent(OrderEvent.record(OrderEventType.RETURN_REJECTED,
                    request.remark() == null ? "退租被拒绝" : request.remark(),
                    request.vendorId()));
            notifyUser(order, "退租被拒", "订单 %s 的退租申请未通过，原因：%s"
                    .formatted(order.getOrderNo(), request.remark() == null ? "无" : request.remark()));
        }
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse applyBuyout(UUID orderId, OrderBuyoutApplyRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureUser(order, request.userId());
        if (order.getStatus() != OrderStatus.IN_LEASE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "当前状态不支持买断");
        }
        order.requestBuyout();
        if (request.buyoutAmount() != null) {
            order.updateBuyoutAmount(request.buyoutAmount());
        }
        order.addEvent(OrderEvent.record(OrderEventType.BUYOUT_REQUESTED,
                request.remark() == null ? "申请买断" : request.remark(),
                request.userId()));
        notifyVendor(order, "收到买断申请", "订单 %s 用户申请买断，请尽快处理。"
            .formatted(order.getOrderNo()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse decideBuyout(UUID orderId, OrderBuyoutDecisionRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureVendor(order, request.vendorId());
        if (order.getStatus() != OrderStatus.BUYOUT_REQUESTED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "没有待处理的买断申请");
        }
        if (request.approve()) {
            order.confirmBuyout();
            order.addEvent(OrderEvent.record(OrderEventType.BUYOUT_CONFIRMED,
                    request.remark() == null ? "买断完成" : request.remark(),
                    request.vendorId()));
            notifyUser(order, "买断完成", "订单 %s 买断成功。".formatted(order.getOrderNo()));
        } else {
            order.rejectBuyout();
            order.addEvent(OrderEvent.record(OrderEventType.BUYOUT_REJECTED,
                    request.remark() == null ? "买断被拒绝" : request.remark(),
                    request.vendorId()));
            notifyUser(order, "买断被拒", "订单 %s 的买断申请未通过，原因：%s"
                    .formatted(order.getOrderNo(), request.remark() == null ? "无" : request.remark()));
        }
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse forceClose(UUID orderId, UUID adminId, String reason) {
        if (adminId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "管理员编号不能为空");
        }
        RentalOrder order = getOrderForUpdate(orderId);
        order.forceClose();
        String message = reason == null || reason.isBlank()
                ? "管理员强制关闭订单"
                : "管理员强制关闭：" + reason;
        order.addEvent(OrderEvent.record(OrderEventType.ORDER_CANCELLED, message, adminId));
        notifyUser(order, "订单已关闭", "订单 %s 已被管理员关闭，原因：%s"
                .formatted(order.getOrderNo(), reason == null || reason.isBlank() ? "无" : reason));
        notifyVendor(order, "订单已关闭", "订单 %s 已被管理员关闭。".formatted(order.getOrderNo()));
        return assembler.toOrderResponse(order);
    }

    private RentalOrder getOrderForUpdate(UUID orderId) {
        return rentalOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    private void ensureUser(RentalOrder order, UUID userId) {
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此订单");
        }
    }

    private void ensureVendor(RentalOrder order, UUID vendorId) {
        if (!order.getVendorId().equals(vendorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此订单");
        }
    }

    private UUID parseTransactionId(String reference) {
        try {
            return UUID.fromString(reference);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付凭证格式错误");
        }
    }

    private void ensurePaymentMatches(RentalOrder order,
                                      PaymentTransactionView transaction,
                                      OrderPaymentRequest request) {
        if (transaction.orderId() == null || !transaction.orderId().equals(order.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付流水与订单不匹配");
        }
        if (transaction.userId() == null || !transaction.userId().equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付用户与订单不一致");
        }
        if (transaction.vendorId() == null || !transaction.vendorId().equals(order.getVendorId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付厂商与订单不一致");
        }
        if (transaction.status() != PaymentStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付尚未完成");
        }
        if (transaction.amount() == null || transaction.amount().compareTo(request.paidAmount()) != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付金额与请求不一致");
        }
        if (transaction.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付金额与订单应付不一致");
        }
    }

    private String buildPaymentMessage(PaymentTransactionView transaction) {
        String reference = transaction.transactionNo() != null
                ? transaction.transactionNo()
                : transaction.id().toString();
        return "支付成功: 交易号 " + reference + ", 金额 " + transaction.amount();
    }

    private Totals calculateTotals(List<OrderItemRequest> items) {
        BigDecimal deposit = BigDecimal.ZERO;
        BigDecimal rent = BigDecimal.ZERO;
        BigDecimal buyout = BigDecimal.ZERO;
        for (OrderItemRequest item : items) {
            BigDecimal quantity = BigDecimal.valueOf(item.quantity());
            deposit = deposit.add(item.unitDepositAmount().multiply(quantity));
            rent = rent.add(item.unitRentAmount().multiply(quantity));
            if (item.buyoutPrice() != null) {
                buyout = buyout.add(item.buyoutPrice().multiply(quantity));
            }
        }
        BigDecimal total = deposit.add(rent);
        return new Totals(deposit, rent, buyout, total);
    }

    private PagedResponse<RentalOrderSummaryResponse> toPagedResponse(Page<RentalOrder> page) {
        return new PagedResponse<>(
                page.stream().map(assembler::toSummary).toList(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private void notifyUser(RentalOrder order, String subject, String content) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                order.getUserId().toString(),
                subject,
                content,
                Map.of("orderNo", order.getOrderNo())
        );
        notificationClient.send(request);
    }

    private void notifyVendor(RentalOrder order, String subject, String content) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                order.getVendorId().toString(),
                subject,
                content,
                Map.of("orderNo", order.getOrderNo())
        );
        notificationClient.send(request);
    }

    private record Totals(BigDecimal depositAmount, BigDecimal rentAmount, BigDecimal buyoutAmount, BigDecimal totalAmount) {
    }
}
