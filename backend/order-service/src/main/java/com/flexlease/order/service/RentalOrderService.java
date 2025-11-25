package com.flexlease.order.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.client.InventoryReservationClient;
import com.flexlease.order.client.InventoryReservationClient.InventoryCommand;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.client.PaymentClient;
import com.flexlease.order.client.PaymentScene;
import com.flexlease.order.client.PaymentStatus;
import com.flexlease.order.client.PaymentTransactionView;
import com.flexlease.order.client.ProductCatalogClient;
import com.flexlease.order.domain.CartItem;
import com.flexlease.order.domain.ExtensionRequestStatus;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderExtensionRequest;
import com.flexlease.order.domain.OrderReturnRequest;
import com.flexlease.order.domain.OrderProofType;
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
import com.flexlease.order.dto.OrderMessageRequest;
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
    private final OrderTimelineService timelineService;
    private final ProductCatalogClient productCatalogClient;
    private final ObjectMapper objectMapper;
    private final CreditAssessmentService creditAssessmentService;

    public RentalOrderService(RentalOrderRepository rentalOrderRepository,
                              OrderExtensionRequestRepository extensionRequestRepository,
                              OrderReturnRequestRepository returnRequestRepository,
                              PaymentClient paymentClient,
                              NotificationClient notificationClient,
                              InventoryReservationClient inventoryReservationClient,
                              CartService cartService,
                              OrderAssembler assembler,
                              OrderTimelineService timelineService,
                              ProductCatalogClient productCatalogClient,
                              ObjectMapper objectMapper,
                              CreditAssessmentService creditAssessmentService) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.extensionRequestRepository = extensionRequestRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.paymentClient = paymentClient;
        this.notificationClient = notificationClient;
        this.inventoryReservationClient = inventoryReservationClient;
        this.cartService = cartService;
        this.assembler = assembler;
        this.timelineService = timelineService;
        this.productCatalogClient = productCatalogClient;
        this.objectMapper = objectMapper;
        this.creditAssessmentService = creditAssessmentService;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderPreviewResponse previewOrder(OrderPreviewRequest request) {
        SecurityUtils.getCurrentUserId().ifPresent(currentUser -> {
            if (request.userId() != null && !currentUser.equals(request.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "请求用户与当前登录用户不一致");
            }
        });
        List<OrderItemRequest> items = request.items() == null ? List.of() : request.items();
        List<ResolvedOrderItem> resolvedItems = resolveOrderItems(request.vendorId(), items);
        Totals totals = calculateTotals(resolvedItems);
        CreditAppliedTotals adjusted = applyCredit(request.userId(), totals);
        CreditAssessmentService.CreditDecision decision = adjusted.decision();
        return new OrderPreviewResponse(
                adjusted.adjustedDeposit(),
                adjusted.rentAmount(),
                adjusted.totalAmount(),
                adjusted.originalDeposit(),
                new OrderPreviewResponse.CreditSnapshot(
                        decision.creditScore(),
                        decision.creditTier(),
                        decision.depositAdjustmentRate(),
                        decision.requiresManualReview()
                )
        );
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

        List<ResolvedOrderItem> resolvedItems = resolveOrderItems(request.vendorId(), orderItems);
        Totals totals = calculateTotals(resolvedItems);
        CreditAppliedTotals adjusted = applyCredit(request.userId(), totals);
        CreditAssessmentService.CreditDecision creditDecision = adjusted.decision();

        RentalOrder order = RentalOrder.create(
                request.userId(),
                request.vendorId(),
                request.planType(),
                adjusted.adjustedDeposit(),
                adjusted.originalDeposit(),
                adjusted.rentAmount(),
                adjusted.buyoutAmount(),
                adjusted.totalAmount(),
                creditDecision.creditScore(),
                creditDecision.creditTier(),
                creditDecision.depositAdjustmentRate(),
                creditDecision.requiresManualReview(),
                request.leaseStartAt(),
                request.leaseEndAt()
        );

        resolvedItems.forEach(item -> order.addItem(toOrderItem(item)));

        List<InventoryCommand> reservationCommands = toInventoryCommands(resolvedItems);
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
        ensureOrderReadable(order);
        return assembler.toOrderResponse(order);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResponse<RentalOrderSummaryResponse> listOrdersForUser(UUID userId, OrderStatus status, Pageable pageable) {
        ensureUserListPermission(userId);
        Page<RentalOrder> page = status == null
                ? rentalOrderRepository.findByUserId(userId, pageable)
                : rentalOrderRepository.findByUserIdAndStatus(userId, status, pageable);
        return toPagedResponse(page);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResponse<RentalOrderSummaryResponse> listOrdersForVendor(UUID vendorId, OrderStatus status, Pageable pageable) {
        ensureVendorListPermission(vendorId);
        Page<RentalOrder> page = status == null
                ? rentalOrderRepository.findByVendorId(vendorId, pageable)
                : rentalOrderRepository.findByVendorIdAndStatus(vendorId, status, pageable);
        return toPagedResponse(page);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResponse<RentalOrderSummaryResponse> listOrdersForAdmin(UUID userId, UUID vendorId, OrderStatus status, Pageable pageable) {
        ensureAdminAccess();
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
        finalizePayment(order, transaction, request.userId(), request.paymentReference(), request.paidAmount());
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse handlePaymentSuccess(UUID orderId, UUID transactionId) {
        if (transactionId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付流水缺失");
        }
        RentalOrder order = getOrderForUpdate(orderId);
        UUID currentTransaction = order.getPaymentTransactionId();
        if (currentTransaction != null && currentTransaction.equals(transactionId)) {
            LOG.debug("Order {} already linked to transaction {}, ignoring duplicate payment callback", order.getOrderNo(), transactionId);
            return assembler.toOrderResponse(order);
        }
        PaymentTransactionView transaction = paymentClient.loadTransaction(transactionId);
        if (currentTransaction == null) {
            ensurePaymentMatches(order, transaction);
            finalizePayment(order, transaction, transaction.userId(), transaction.transactionNo(), transaction.amount());
        } else {
            ensureSupplementalPayment(order, transaction);
            recordSupplementalPayment(order, transaction);
        }
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
        boolean hasShipmentProof = order.getProofs().stream()
                .anyMatch(proof -> proof.getProofType() == OrderProofType.SHIPMENT);
        if (!hasShipmentProof) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请先上传发货凭证后再提交物流信息");
        }
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
        boolean hasReceiveProof = order.getProofs().stream()
                .anyMatch(proof -> proof.getProofType() == OrderProofType.RECEIVE);
        if (!hasReceiveProof) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请先上传收货凭证后再确认收货");
        }
        order.confirmReceive();
        recordEvent(order, OrderEventType.ORDER_RECEIVED, "用户确认收货", request.actorId());
        notifyVendor(order, "买家确认收货", "订单 %s 已确认收货，租期正式开始计算。".formatted(order.getOrderNo()));
        return assembler.toOrderResponse(order);
    }

    public RentalOrderResponse postConversationMessage(UUID orderId, OrderMessageRequest request) {
        RentalOrder order = getOrderForUpdate(orderId);
        if (request.actorId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "缺少操作人");
        }
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "留言内容不能为空");
        }
        OrderActorRole actorRole = resolveActorRole(order, request.actorId());
        recordEvent(order, OrderEventType.COMMUNICATION_NOTE, message, request.actorId(), Map.of(), actorRole);
        String snippet = message.length() > 120 ? message.substring(0, 120) + "..." : message;
        if (actorRole == OrderActorRole.USER) {
            notifyVendor(order, "收到用户留言", "订单 %s 有新的留言：%s".formatted(order.getOrderNo(), snippet));
        } else if (actorRole == OrderActorRole.VENDOR) {
            notifyUser(order, "厂商回复", "订单 %s 有新的厂商回复：%s".formatted(order.getOrderNo(), snippet));
        }
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

    private void ensureOrderReadable(RentalOrder order) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        UUID principalUserId = principal.userId();
        if (principal.hasRole("VENDOR")) {
            UUID currentVendorId = principal.vendorId();
            if (currentVendorId == null || !currentVendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单");
            }
            return;
        }
        if (principal.hasRole("USER")) {
            if (principalUserId == null || !principalUserId.equals(order.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单");
            }
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少访问订单详情的权限");
    }

    private void ensureUserListPermission(UUID userId) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        if (principal.userId() == null || !principal.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该用户的订单");
        }
    }

    private void ensureVendorListPermission(UUID vendorId) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        if (!principal.hasRole("VENDOR")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅厂商可访问厂商订单");
        }
        UUID currentVendorId = principal.vendorId();
        if (currentVendorId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
        }
        if (!currentVendorId.equals(vendorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该厂商的订单");
        }
    }

    private void ensureAdminAccess() {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN") && !principal.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行该操作");
        }
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
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            if (vendorId != null && !vendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "厂商信息不匹配");
            }
            return;
        }
        if (!principal.hasRole("VENDOR")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份不允许执行此操作");
        }
        UUID currentVendorId = principal.vendorId();
        if (currentVendorId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
        }
        if (vendorId != null && !vendorId.equals(currentVendorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "厂商信息不匹配");
        }
        if (!order.getVendorId().equals(currentVendorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
    }

    private OrderActorRole resolveActorRole(RentalOrder order, UUID actorId) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        UUID currentUserId = principal.userId();
        if (currentUserId == null || !currentUserId.equals(actorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请求用户与当前登录用户不一致");
        }
        if (principal.hasRole("ADMIN")) {
            return OrderActorRole.ADMIN;
        }
        if (principal.hasRole("INTERNAL")) {
            return OrderActorRole.INTERNAL;
        }
        if (principal.hasRole("VENDOR")) {
            UUID vendorId = principal.vendorId();
            if (vendorId == null || !vendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该订单");
            }
            return OrderActorRole.VENDOR;
        }
        if (principal.hasRole("USER")) {
            if (!order.getUserId().equals(actorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该订单");
            }
            return OrderActorRole.USER;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少执行该操作的权限");
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
        ensurePaymentMatches(order, transaction);
        if (request.paidAmount() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请求缺少支付金额");
        }
        if (transaction.amount().compareTo(request.paidAmount()) != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付金额与请求不一致");
        }
    }

    private void ensurePaymentMatches(RentalOrder order, PaymentTransactionView transaction) {
        ensurePaymentBelongsToOrder(order, transaction);
        if (transaction.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付金额与订单应付不一致");
        }
    }

    private void ensureSupplementalPayment(RentalOrder order, PaymentTransactionView transaction) {
        ensurePaymentBelongsToOrder(order, transaction);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXCEPTION_CLOSED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单已关闭，无法记录补款");
        }
    }

    private void ensurePaymentBelongsToOrder(RentalOrder order, PaymentTransactionView transaction) {
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
        if (transaction.amount() == null || transaction.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付金额无效");
        }
    }

    private boolean finalizePayment(RentalOrder order,
                                     PaymentTransactionView transaction,
                                     UUID actorId,
                                     String paymentReference,
                                     BigDecimal paidAmount) {
        if (order.getPaymentTransactionId() != null) {
            if (order.getPaymentTransactionId().equals(transaction.id())) {
                LOG.debug("Order {} already finalized with transaction {}", order.getOrderNo(), transaction.id());
                return false;
            }
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单已关联其他支付流水");
        }
        try {
            order.markPaid();
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单状态不支持支付确认");
        }
        order.setPaymentTransactionId(transaction.id());
        Map<String, Object> attributes = new HashMap<>();
        if (paymentReference != null && !paymentReference.isBlank()) {
            attributes.put("paymentReference", paymentReference);
        }
        BigDecimal notificationAmount = paidAmount != null ? paidAmount : transaction.amount();
        if (notificationAmount != null) {
            attributes.put("paidAmount", notificationAmount);
        }
        recordEvent(order,
                OrderEventType.PAYMENT_CONFIRMED,
                buildPaymentMessage(transaction),
                actorId,
                attributes);
        if (notificationAmount != null) {
            notifyUser(order,
                    "订单支付成功",
                    "订单 %s 支付成功，实付 ¥%s。".formatted(order.getOrderNo(), notificationAmount));
        } else {
            notifyUser(order, "订单支付成功", "订单 %s 支付成功。".formatted(order.getOrderNo()));
        }
        notifyVendor(order, "订单待发货", "订单 %s 已完成支付，请尽快安排发货。".formatted(order.getOrderNo()));
        return true;
    }

    private void recordSupplementalPayment(RentalOrder order, PaymentTransactionView transaction) {
        BigDecimal amount = transaction.amount() != null ? transaction.amount() : BigDecimal.ZERO;
        String sceneLabel = paymentSceneLabel(transaction.scene());
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("transactionId", transaction.id().toString());
        attributes.put("amount", amount);
        if (transaction.scene() != null) {
            attributes.put("scene", transaction.scene().name());
        }
        if (transaction.transactionNo() != null && !transaction.transactionNo().isBlank()) {
            attributes.put("paymentReference", transaction.transactionNo());
        }
        recordEvent(order,
                OrderEventType.ADDITIONAL_PAYMENT_RECORDED,
                "%s支付完成: ¥%s".formatted(sceneLabel, amount),
                transaction.userId(),
                attributes,
                resolvePaymentActorRole(order, transaction));
        notifyVendor(order,
                "收到%s补款".formatted(sceneLabel),
                "订单 %s 收到 %s 补款 ¥%s。".formatted(order.getOrderNo(), sceneLabel, amount));
        notifyUser(order,
                "%s补款成功".formatted(sceneLabel),
                "订单 %s 的 %s 支付 ¥%s 已完成。".formatted(order.getOrderNo(), sceneLabel, amount));
    }

    private String buildPaymentMessage(PaymentTransactionView transaction) {
        String reference = transaction.transactionNo() != null
                ? transaction.transactionNo()
                : transaction.id().toString();
        return "支付成功: 交易号 " + reference + ", 金额 " + transaction.amount();
    }

    private String paymentSceneLabel(PaymentScene scene) {
        if (scene == null) {
            return "补款";
        }
        return switch (scene) {
            case DEPOSIT -> "押金";
            case RENT -> "租金";
            case BUYOUT -> "买断款";
            case PENALTY -> "违约金";
        };
    }

    private OrderActorRole resolvePaymentActorRole(RentalOrder order, PaymentTransactionView transaction) {
        if (transaction.userId() != null && transaction.userId().equals(order.getUserId())) {
            return OrderActorRole.USER;
        }
        if (transaction.vendorId() != null && transaction.vendorId().equals(order.getVendorId())) {
            return OrderActorRole.VENDOR;
        }
        return OrderActorRole.INTERNAL;
    }

    private Totals calculateTotals(List<ResolvedOrderItem> items) {
        BigDecimal deposit = BigDecimal.ZERO;
        BigDecimal rent = BigDecimal.ZERO;
        BigDecimal buyout = BigDecimal.ZERO;
        for (ResolvedOrderItem item : items) {
            BigDecimal quantity = BigDecimal.valueOf(item.quantity());
            deposit = deposit.add(item.unitDepositAmount().multiply(quantity));
            rent = rent.add(item.unitRentAmount().multiply(quantity));
            if (item.buyoutPrice() != null) {
                buyout = buyout.add(item.buyoutPrice().multiply(quantity));
            }
        }
        return new Totals(deposit, rent, buyout);
    }

    private CreditAppliedTotals applyCredit(UUID userId, Totals totals) {
        CreditAssessmentService.CreditDecision decision = creditAssessmentService.assess(userId);
        BigDecimal adjustedDeposit = decision.apply(totals.depositAmount);
        BigDecimal totalAmount = adjustedDeposit.add(totals.rentAmount);
        return new CreditAppliedTotals(
                totals.depositAmount,
                adjustedDeposit,
                totals.rentAmount,
                totals.buyoutAmount,
                totalAmount,
                decision
        );
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

    private RentalOrderItem toOrderItem(ResolvedOrderItem item) {
        return RentalOrderItem.create(
                item.productId(),
                item.skuId(),
                item.planId(),
                item.productName(),
                item.skuCode(),
                item.planSnapshot(),
                item.quantity(),
                item.unitRentAmount(),
                item.unitDepositAmount(),
                item.buyoutPrice()
        );
    }

    private List<InventoryCommand> toInventoryCommands(List<ResolvedOrderItem> items) {
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
        recordEvent(order, eventType, description, actorId, Map.of(), null);
    }

    private void recordEvent(RentalOrder order,
                             OrderEventType eventType,
                             String description,
                             UUID actorId,
                             Map<String, Object> attributes) {
        recordEvent(order, eventType, description, actorId, attributes, null);
    }

    private void recordEvent(RentalOrder order,
                             OrderEventType eventType,
                             String description,
                             UUID actorId,
                             Map<String, Object> attributes,
                             OrderActorRole actorRole) {
        timelineService.append(order, eventType, description, actorId, attributes, actorRole);
    }

    private record Totals(BigDecimal depositAmount, BigDecimal rentAmount, BigDecimal buyoutAmount) {
    }

    private record CreditAppliedTotals(BigDecimal originalDeposit,
                                       BigDecimal adjustedDeposit,
                                       BigDecimal rentAmount,
                                       BigDecimal buyoutAmount,
                                       BigDecimal totalAmount,
                                       CreditAssessmentService.CreditDecision decision) {
    }

    private List<ResolvedOrderItem> resolveOrderItems(UUID expectedVendorId, List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<UUID, ProductCatalogClient.CatalogProductView> products = loadProductsForItems(expectedVendorId, items);
        return items.stream()
                .map(item -> resolveOrderItem(item, expectedVendorId, products))
                .toList();
    }

    private Map<UUID, ProductCatalogClient.CatalogProductView> loadProductsForItems(UUID expectedVendorId,
                                                                                   List<OrderItemRequest> items) {
        Map<UUID, ProductCatalogClient.CatalogProductView> products = new HashMap<>();
        for (OrderItemRequest item : items) {
            ProductCatalogClient.CatalogProductView product = products.computeIfAbsent(
                    item.productId(),
                    productCatalogClient::getProduct
            );
            if (!expectedVendorId.equals(product.vendorId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单明细所属厂商与请求不一致");
            }
        }
        return products;
    }

    private ResolvedOrderItem resolveOrderItem(OrderItemRequest item,
                                               UUID expectedVendorId,
                                               Map<UUID, ProductCatalogClient.CatalogProductView> products) {
        ProductCatalogClient.CatalogProductView product = products.get(item.productId());
        if (product == null) {
            product = productCatalogClient.getProduct(item.productId());
            if (!expectedVendorId.equals(product.vendorId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单明细所属厂商与请求不一致");
            }
            products.put(item.productId(), product);
        }

        ProductCatalogClient.CatalogProductView.RentalPlanView plan = selectPlan(item, product);
        ProductCatalogClient.CatalogProductView.SkuView sku = selectSku(item, product, plan);

        PlanSnapshotData snapshot = parsePlanSnapshot(item.planSnapshot()).orElse(null);
        if (snapshot == null && item.planId() != null) {
            snapshot = new PlanSnapshotData(item.planId(), null, null, null, null, null);
        }
        PlanSnapshotData mergedSnapshot = mergeSnapshotData(snapshot, plan);
        if (mergedSnapshot.planId() == null && item.planId() != null) {
            mergedSnapshot = new PlanSnapshotData(item.planId(),
                    mergedSnapshot.planType(),
                    mergedSnapshot.termMonths(),
                    mergedSnapshot.depositAmount(),
                    mergedSnapshot.rentAmountMonthly(),
                    mergedSnapshot.buyoutPrice());
        }

        BigDecimal unitDeposit = mergedSnapshot.depositAmount();
        BigDecimal unitRent = mergedSnapshot.rentAmountMonthly();
        if (unitDeposit == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法确定押金金额，请刷新商品数据后重试");
        }
        if (unitRent == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法确定租金金额，请刷新商品数据后重试");
        }

        String productName = product.name() != null && !product.name().isBlank() ? product.name() : item.productName();
        String skuCode = sku != null && sku.skuCode() != null && !sku.skuCode().isBlank()
                ? sku.skuCode()
                : item.skuCode();
        UUID resolvedPlanId = plan != null ? plan.id() : mergedSnapshot.planId();
        UUID resolvedSkuId = sku != null ? sku.id() : item.skuId();
        String sanitizedSnapshot = writePlanSnapshot(mergedSnapshot, item.planSnapshot());

        return new ResolvedOrderItem(
                item.productId(),
                resolvedSkuId,
                resolvedPlanId,
                productName,
                skuCode,
                sanitizedSnapshot,
                item.quantity(),
                unitRent,
                unitDeposit,
                mergedSnapshot.buyoutPrice()
        );
    }

    private ProductCatalogClient.CatalogProductView.RentalPlanView selectPlan(OrderItemRequest item,
                                                                              ProductCatalogClient.CatalogProductView product) {
        List<ProductCatalogClient.CatalogProductView.RentalPlanView> plans = product.rentalPlans();
        if (plans == null || plans.isEmpty()) {
            return null;
        }
        if (item.planId() != null) {
            return plans.stream()
                    .filter(plan -> item.planId().equals(plan.id()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "租赁方案不存在: " + item.planId()));
        }
        if (item.skuId() != null) {
            return plans.stream()
                    .filter(plan -> plan.skus() != null && plan.skus().stream().anyMatch(sku -> item.skuId().equals(sku.id())))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private ProductCatalogClient.CatalogProductView.SkuView selectSku(OrderItemRequest item,
                                                                      ProductCatalogClient.CatalogProductView product,
                                                                      ProductCatalogClient.CatalogProductView.RentalPlanView plan) {
        if (item.skuId() == null) {
            return null;
        }
        if (plan != null && plan.skus() != null) {
            return plan.skus().stream()
                    .filter(sku -> item.skuId().equals(sku.id()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "SKU 不属于指定租赁方案"));
        }
        List<ProductCatalogClient.CatalogProductView.RentalPlanView> plans = product.rentalPlans();
        if (plans == null || plans.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SKU 不属于指定商品");
        }
        return plans.stream()
                .filter(p -> p.skus() != null)
                .flatMap(p -> p.skus().stream())
                .filter(sku -> item.skuId().equals(sku.id()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "SKU 不属于指定商品"));
    }

    private PlanSnapshotData mergeSnapshotData(PlanSnapshotData snapshot,
                                               ProductCatalogClient.CatalogProductView.RentalPlanView plan) {
        UUID planId = plan != null ? plan.id() : snapshot != null ? snapshot.planId() : null;
        String planType = snapshot != null && snapshot.planType() != null
                ? snapshot.planType()
                : plan != null ? plan.planType() : null;
        Integer termMonths = snapshot != null && snapshot.termMonths() != null
                ? snapshot.termMonths()
                : plan != null ? plan.termMonths() : null;
        BigDecimal deposit = snapshot != null && snapshot.depositAmount() != null
                ? snapshot.depositAmount()
                : plan != null ? plan.depositAmount() : null;
        BigDecimal rent = snapshot != null && snapshot.rentAmountMonthly() != null
                ? snapshot.rentAmountMonthly()
                : plan != null ? plan.rentAmountMonthly() : null;
        BigDecimal buyout = snapshot != null && snapshot.buyoutPrice() != null
                ? snapshot.buyoutPrice()
                : plan != null ? plan.buyoutPrice() : null;
        return new PlanSnapshotData(planId, planType, termMonths, deposit, rent, buyout);
    }

    private Optional<PlanSnapshotData> parsePlanSnapshot(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, PlanSnapshotData.class));
        } catch (JsonProcessingException ex) {
            LOG.debug("Failed to parse plan snapshot: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String writePlanSnapshot(PlanSnapshotData snapshot, String fallback) {
        if (snapshot == null) {
            return fallback;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            LOG.warn("Failed to serialize plan snapshot: {}", ex.getMessage());
            return fallback;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlanSnapshotData(
            UUID planId,
            String planType,
            Integer termMonths,
            BigDecimal depositAmount,
            BigDecimal rentAmountMonthly,
            BigDecimal buyoutPrice
    ) {
    }

    private record ResolvedOrderItem(
            UUID productId,
            UUID skuId,
            UUID planId,
            String productName,
            String skuCode,
            String planSnapshot,
            int quantity,
            BigDecimal unitRentAmount,
            BigDecimal unitDepositAmount,
            BigDecimal buyoutPrice
    ) {
    }
}
