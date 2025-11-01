package com.flexlease.order.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.order.client.InventoryReservationClient;
import com.flexlease.order.client.InventoryReservationClient.InventoryCommand;
import com.flexlease.order.client.PaymentClient;
import com.flexlease.order.client.PaymentStatus;
import com.flexlease.order.client.PaymentTransactionView;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.domain.CartItem;
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
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.repository.OrderExtensionRequestRepository;
import com.flexlease.order.repository.OrderReturnRequestRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class RentalOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(RentalOrderService.class);

    private final RentalOrderRepository rentalOrderRepository;
    private final OrderExtensionRequestRepository extensionRequestRepository;
    private final OrderReturnRequestRepository returnRequestRepository;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;
    private final InventoryReservationClient inventoryReservationClient;
    private final CartService cartService;
    private final OrderAssembler assembler;
    private final OrderEventPublisher orderEventPublisher;

    public RentalOrderService(RentalOrderRepository rentalOrderRepository,
                              OrderExtensionRequestRepository extensionRequestRepository,
                              OrderReturnRequestRepository returnRequestRepository,
                              PaymentClient paymentClient,
                              NotificationClient notificationClient,
                              InventoryReservationClient inventoryReservationClient,
                              CartService cartService,
                              OrderAssembler assembler,
                              OrderEventPublisher orderEventPublisher) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.extensionRequestRepository = extensionRequestRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.paymentClient = paymentClient;
        this.notificationClient = notificationClient;
        this.inventoryReservationClient = inventoryReservationClient;
        this.cartService = cartService;
        this.assembler = assembler;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderPreviewResponse previewOrder(OrderPreviewRequest request) {
        SecurityUtils.getCurrentUserId().ifPresent(currentUser -> {
            if (request.userId() != null && !currentUser.equals(request.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "请求用户与当前登录用户不一致");
            }
        });
        Totals totals = calculateTotals(request.items());
        return new OrderPreviewResponse(totals.depositAmount, totals.rentAmount, totals.totalAmount);
    }

    public RentalOrderResponse createOrder(CreateOrderRequest request) {
        SecurityUtils.getCurrentUserId().ifPresent(currentUser -> {
            if (!currentUser.equals(request.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "请求用户与当前登录用户不一致");
            }
        });
        List<OrderItemRequest> directItems = request.items() == null ? List.of() : request.items();
        List<UUID> cartItemIds = request.cartItemIds() == null ? List.of() : request.cartItemIds();
        if (!directItems.isEmpty() && !cartItemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请勿同时提供购物车条目和订单明细");
        }

        List<CartItem> cartItems = List.of();
        List<OrderItemRequest> orderItems = directItems;
        if (orderItems.isEmpty()) {
            cartItems = cartService.loadCartItems(request.userId(), cartItemIds);
            if (cartItems.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单缺少商品明细");
            }
            long vendorCount = cartItems.stream().map(CartItem::getVendorId).distinct().count();
            if (vendorCount != 1) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "购物车条目属于不同厂商，无法合并下单");
            }
            UUID cartVendorId = cartItems.get(0).getVendorId();
            if (!cartVendorId.equals(request.vendorId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "厂商信息不一致");
            }
            orderItems = cartItems.stream().map(this::toOrderItemRequest).toList();
        }

        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单缺少商品明细");
        }

        Totals totals = calculateTotals(orderItems);

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

        orderItems.forEach(itemRequest -> order.addItem(toOrderItem(itemRequest)));

        List<InventoryCommand> reservationCommands = toInventoryCommands(orderItems);
        if (!reservationCommands.isEmpty()) {
            inventoryReservationClient.reserve(order.getId(), reservationCommands);
        }
        try {
            recordEvent(order, OrderEventType.ORDER_CREATED, "订单创建", request.userId());
            RentalOrder saved = rentalOrderRepository.save(order);
            if (!cartItems.isEmpty()) {
                cartService.removeItems(request.userId(), cartItemIds);
            }
            return assembler.toOrderResponse(saved);
        } catch (RuntimeException ex) {
            if (!reservationCommands.isEmpty()) {
                try {
                    inventoryReservationClient.release(order.getId(), reservationCommands);
                } catch (RuntimeException ignored) {
                    // release failure will be surfaced via monitoring, original exception still thrown
                }
            }
            throw ex;
        }
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
        order.setPaymentTransactionId(transaction.id());
        recordEvent(order,
                OrderEventType.PAYMENT_CONFIRMED,
                buildPaymentMessage(transaction),
                request.userId(),
                Map.of(
                        "paymentReference", request.paymentReference(),
                        "paidAmount", request.paidAmount()
                ));
        notifyUser(order, "订单支付成功", "订单 %s 支付成功，实付 ¥%s。".formatted(order.getOrderNo(), request.paidAmount()));
        notifyVendor(order, "订单待发货", "订单 %s 已完成支付，请尽快安排发货。".formatted(order.getOrderNo()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse cancelOrder(UUID orderId, OrderCancelRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureUser(order, request.userId());
        order.cancel();
        recordEvent(order, OrderEventType.ORDER_CANCELLED,
                request.reason() == null ? "用户取消订单" : request.reason(),
                request.userId());
        notifyVendor(order, "订单已取消", "订单 %s 已被用户取消。".formatted(order.getOrderNo()));
        releaseReservedInventory(order);
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse shipOrder(UUID orderId, OrderShipmentRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureVendor(order, request.vendorId());
        List<InventoryCommand> commands = buildInventoryCommands(order);
        boolean outboundDone = false;
        boolean releaseDone = false;
        try {
            order.ship(request.carrier(), request.trackingNumber());
            if (!commands.isEmpty()) {
                inventoryReservationClient.outbound(order.getId(), commands);
                outboundDone = true;
                inventoryReservationClient.release(order.getId(), commands);
                releaseDone = true;
            }
        } catch (RuntimeException ex) {
            if (releaseDone && !commands.isEmpty()) {
                try {
                    inventoryReservationClient.reserve(order.getId(), commands);
                } catch (RuntimeException compensationEx) {
                    LOG.warn("Failed to compensate inventory reserve for order {}: {}", order.getOrderNo(), compensationEx.getMessage());
                }
            }
            if (outboundDone && !commands.isEmpty()) {
                try {
                    inventoryReservationClient.inbound(order.getId(), commands);
                } catch (RuntimeException compensationEx) {
                    LOG.warn("Failed to compensate inventory inbound for order {}: {}", order.getOrderNo(), compensationEx.getMessage());
                }
            }
            throw ex;
        }
        Map<String, Object> shipmentAttributes = Map.of(
                "carrier", request.carrier(),
                "trackingNumber", request.trackingNumber()
        );
        recordEvent(order, OrderEventType.ORDER_SHIPPED,
                "发货信息: " + request.carrier() + " / " + request.trackingNumber(),
                request.vendorId(),
                shipmentAttributes);
        notifyUser(order, "订单已发货", "订单 %s 已发货，承运方 %s，运单号 %s。"
            .formatted(order.getOrderNo(), request.carrier(), request.trackingNumber()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse confirmReceive(UUID orderId, OrderActorRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        ensureUser(order, request.actorId());
        order.confirmReceive();
        recordEvent(order, OrderEventType.ORDER_RECEIVED, "用户确认收货", request.actorId());
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
        recordEvent(order, OrderEventType.EXTENSION_REQUESTED,
                "续租申请: " + request.additionalMonths() + " 个月",
                request.userId(),
                Map.of("additionalMonths", request.additionalMonths()));
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
            recordEvent(order, OrderEventType.EXTENSION_APPROVED,
                    "续租通过: " + extensionRequest.getAdditionalMonths() + " 个月",
                    request.vendorId(),
                    Map.of("additionalMonths", extensionRequest.getAdditionalMonths()));
            notifyUser(order, "续租申请通过", "订单 %s 续租成功，追加 %d 个月租期。"
                .formatted(order.getOrderNo(), extensionRequest.getAdditionalMonths()));
        } else {
            extensionRequest.reject(request.vendorId(), request.remark());
            recordEvent(order, OrderEventType.EXTENSION_REJECTED,
                    request.remark() == null ? "续租申请被拒绝" : request.remark(),
                    request.vendorId());
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
        Map<String, Object> returnAttributes = new HashMap<>();
        if (request.logisticsCompany() != null && !request.logisticsCompany().isBlank()) {
            returnAttributes.put("logisticsCompany", request.logisticsCompany());
        }
        if (request.trackingNumber() != null && !request.trackingNumber().isBlank()) {
            returnAttributes.put("trackingNumber", request.trackingNumber());
        }
        recordEvent(order, OrderEventType.RETURN_REQUESTED,
                request.reason() == null ? "发起退租" : request.reason(),
                request.userId(),
                returnAttributes);
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
            List<InventoryCommand> commands = buildInventoryCommands(order);
            boolean inboundDone = false;
            BigDecimal refundedAmount = BigDecimal.ZERO;
            try {
                if (!commands.isEmpty()) {
                    inventoryReservationClient.inbound(order.getId(), commands);
                    inboundDone = true;
                }
                refundedAmount = issueDepositRefundIfNeeded(order);
            } catch (RuntimeException ex) {
                if (inboundDone && !commands.isEmpty()) {
                    try {
                        inventoryReservationClient.outbound(order.getId(), commands);
                    } catch (RuntimeException compensationEx) {
                        LOG.warn("Failed to compensate inventory outbound for order {} after inbound: {}", order.getOrderNo(), compensationEx.getMessage());
                    }
                }
                throw ex;
            }
            order.completeReturn();
            Map<String, Object> attributes = new HashMap<>();
            if (request.remark() != null && !request.remark().isBlank()) {
                attributes.put("remark", request.remark());
            }
            if (refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
                attributes.put("refundAmount", refundedAmount);
            }
            recordEvent(order, OrderEventType.RETURN_APPROVED,
                    request.remark() == null ? "退租完成" : request.remark(),
                    request.vendorId(),
                    attributes);
            notifyUser(order, "退租已完成", "订单 %s 的退租申请已通过。".formatted(order.getOrderNo()));
        } else {
            returnRequest.reject(request.vendorId(), request.remark());
            order.resumeLease();
            recordEvent(order, OrderEventType.RETURN_REJECTED,
                    request.remark() == null ? "退租被拒绝" : request.remark(),
                    request.vendorId());
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
        recordEvent(order, OrderEventType.BUYOUT_REQUESTED,
                request.remark() == null ? "申请买断" : request.remark(),
                request.userId(),
                request.buyoutAmount() == null ? Map.of() : Map.of("buyoutAmount", request.buyoutAmount()));
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
            recordEvent(order, OrderEventType.BUYOUT_CONFIRMED,
                    request.remark() == null ? "买断完成" : request.remark(),
                    request.vendorId());
            notifyUser(order, "买断完成", "订单 %s 买断成功。".formatted(order.getOrderNo()));
        } else {
            order.rejectBuyout();
            recordEvent(order, OrderEventType.BUYOUT_REJECTED,
                    request.remark() == null ? "买断被拒绝" : request.remark(),
                    request.vendorId());
            notifyUser(order, "买断被拒", "订单 %s 的买断申请未通过，原因：%s"
                    .formatted(order.getOrderNo(), request.remark() == null ? "无" : request.remark()));
        }
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse forceClose(UUID orderId, String reason) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以执行此操作");
        }
        UUID adminId = SecurityUtils.requireUserId();
        RentalOrder order = getOrderForUpdate(orderId);
        OrderStatus previousStatus = order.getStatus();
        order.forceClose();
        String message = reason == null || reason.isBlank()
                ? "管理员强制关闭订单"
                : "管理员强制关闭：" + reason;
        recordEvent(order, OrderEventType.ORDER_CANCELLED, message, adminId);
        notifyUser(order, "订单已关闭", "订单 %s 已被管理员关闭，原因：%s"
                .formatted(order.getOrderNo(), reason == null || reason.isBlank() ? "无" : reason));
        notifyVendor(order, "订单已关闭", "订单 %s 已被管理员关闭。".formatted(order.getOrderNo()));
        if (previousStatus == OrderStatus.PENDING_PAYMENT || previousStatus == OrderStatus.AWAITING_SHIPMENT) {
            releaseReservedInventory(order);
        }
        return assembler.toOrderResponse(order);
    }

    private RentalOrder getOrderForUpdate(UUID orderId) {
        return rentalOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    private void ensureUser(RentalOrder order, UUID userId) {
        SecurityUtils.getCurrentUserId().ifPresentOrElse(currentUserId -> {
            if (userId != null && !userId.equals(currentUserId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "请求用户与当前登录用户不一致");
            }
            if (!order.getUserId().equals(currentUserId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此订单");
            }
        }, () -> {
            if (userId != null && !order.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此订单");
            }
        });
    }

    private void ensureVendor(RentalOrder order, UUID vendorId) {
        Optional<FlexleasePrincipal> principal = SecurityUtils.getCurrentPrincipal();
        principal.ifPresent(p -> {
            if (!p.hasRole("VENDOR") && !p.hasRole("ADMIN")) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份不允许执行此操作");
            }
        });
        if (principal.isPresent() || vendorId != null) {
            UUID expectedVendorId = vendorId != null ? vendorId : order.getVendorId();
            if (!order.getVendorId().equals(expectedVendorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "厂商信息不匹配");
            }
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
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to send user notification '{}' for order {}: {}", subject, order.getOrderNo(), ex.getMessage());
        }
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
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to send vendor notification '{}' for order {}: {}", subject, order.getOrderNo(), ex.getMessage());
        }
    }

    private OrderItemRequest toOrderItemRequest(CartItem cartItem) {
        return new OrderItemRequest(
                cartItem.getProductId(),
                cartItem.getSkuId(),
                cartItem.getPlanId(),
                cartItem.getProductName(),
                cartItem.getSkuCode(),
                cartItem.getPlanSnapshot(),
                cartItem.getQuantity(),
                cartItem.getUnitRentAmount(),
                cartItem.getUnitDepositAmount(),
                cartItem.getBuyoutPrice()
        );
    }

    private RentalOrderItem toOrderItem(OrderItemRequest itemRequest) {
        return RentalOrderItem.create(
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
    }

    private List<InventoryCommand> toInventoryCommands(List<OrderItemRequest> items) {
        return items.stream()
                .map(item -> {
                    if (item.skuId() == null) {
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "缺少 SKU 信息，无法预占库存");
                    }
                    return new InventoryCommand(item.skuId(), item.quantity());
                })
                .toList();
    }

    private List<InventoryCommand> buildInventoryCommands(RentalOrder order) {
        return order.getItems().stream()
                .filter(item -> item.getSkuId() != null)
                .map(item -> new InventoryCommand(item.getSkuId(), item.getQuantity()))
                .toList();
    }

    private BigDecimal issueDepositRefundIfNeeded(RentalOrder order) {
        if (order.getPaymentTransactionId() == null) {
            return BigDecimal.ZERO;
        }
        if (order.getDepositAmount() == null || order.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        PaymentTransactionView transaction = paymentClient.loadTransaction(order.getPaymentTransactionId());
        if (transaction.orderId() != null && !transaction.orderId().equals(order.getId())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付流水与订单信息不匹配，无法退款");
        }
        if (transaction.status() != PaymentStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付流水状态异常，无法退款");
        }
        BigDecimal refunded = transaction.refunds() == null
                ? BigDecimal.ZERO
                : transaction.refunds().stream()
                .map(PaymentTransactionView.RefundView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deposit = order.getDepositAmount();
        BigDecimal remaining = deposit.subtract(refunded);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        paymentClient.createRefund(order.getPaymentTransactionId(), remaining, "订单退租押金退款");
        return remaining;
    }

    private void releaseReservedInventory(RentalOrder order) {
        List<InventoryCommand> commands = buildInventoryCommands(order);
        if (!commands.isEmpty()) {
            inventoryReservationClient.release(order.getId(), commands);
        }
    }

    private void recordEvent(RentalOrder order,
                             OrderEventType eventType,
                             String description,
                             UUID actorId) {
        recordEvent(order, eventType, description, actorId, Map.of());
    }

    private void recordEvent(RentalOrder order,
                             OrderEventType eventType,
                             String description,
                             UUID actorId,
                             Map<String, Object> attributes) {
        order.addEvent(OrderEvent.record(eventType, description, actorId));
        orderEventPublisher.publish(order, eventType, description, actorId, attributes);
    }

    private record Totals(BigDecimal depositAmount, BigDecimal rentAmount, BigDecimal buyoutAmount, BigDecimal totalAmount) {
    }
}
