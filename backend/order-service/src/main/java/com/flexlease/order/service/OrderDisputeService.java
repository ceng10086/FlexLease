package com.flexlease.order.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.client.UserProfileClient;
import com.flexlease.order.domain.DisputeResolutionOption;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.OrderDisputeStatus;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderProof;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.dto.OrderDisputeAppealRequest;
import com.flexlease.order.dto.OrderDisputeCreateRequest;
import com.flexlease.order.dto.OrderDisputeEscalateRequest;
import com.flexlease.order.dto.OrderDisputeResolveRequest;
import com.flexlease.order.dto.OrderDisputeResponse;
import com.flexlease.order.dto.OrderDisputeResponseRequest;
import com.flexlease.order.repository.OrderDisputeRepository;
import com.flexlease.order.repository.OrderProofRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单纠纷服务：支持发起纠纷、协商回应、升级仲裁以及平台裁决/申诉。
 * <p>
 * 纠纷的每一步都会写入订单时间线，并通过 notification-service 推送站内信提醒对方；
 * 同时可联动信用事件与满意度调研调度（协商达成一致或平台裁决后触发）。
 */
@Service
@Transactional
public class OrderDisputeService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderDisputeService.class);
    private static final String DISPUTE_CONTEXT = "DISPUTE";
    private static final String CREDIT_CONTEXT = "CREDIT";
    private static final int MAX_ATTACHMENTS = 5;

    private final RentalOrderRepository rentalOrderRepository;
    private final OrderDisputeRepository orderDisputeRepository;
    private final OrderProofRepository orderProofRepository;
    private final OrderAssembler orderAssembler;
    private final OrderTimelineService timelineService;
    private final NotificationClient notificationClient;
    private final UserProfileClient userProfileClient;
    private final OrderSurveyService orderSurveyService;
    private final CreditRewardService creditRewardService;

    public OrderDisputeService(RentalOrderRepository rentalOrderRepository,
                               OrderDisputeRepository orderDisputeRepository,
                               OrderProofRepository orderProofRepository,
                               OrderAssembler orderAssembler,
                               OrderTimelineService timelineService,
                               NotificationClient notificationClient,
                               UserProfileClient userProfileClient,
                               OrderSurveyService orderSurveyService,
                               CreditRewardService creditRewardService) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.orderDisputeRepository = orderDisputeRepository;
        this.orderProofRepository = orderProofRepository;
        this.orderAssembler = orderAssembler;
        this.timelineService = timelineService;
        this.notificationClient = notificationClient;
        this.userProfileClient = userProfileClient;
        this.orderSurveyService = orderSurveyService;
        this.creditRewardService = creditRewardService;
    }

    public List<OrderDisputeResponse> list(UUID orderId) {
        RentalOrder order = loadOrder(orderId);
        ensureReadable(order);
        return order.getDisputes().stream()
                .sorted(Comparator.comparing(OrderDispute::getCreatedAt))
                .map(orderAssembler::toDisputeResponse)
                .toList();
    }

    public OrderDisputeResponse create(UUID orderId, OrderDisputeCreateRequest request) {
        RentalOrder order = loadOrder(orderId);
        boolean hasActiveDispute = order.getDisputes().stream()
                .anyMatch(dispute -> dispute.getStatus() != OrderDisputeStatus.CLOSED);
        if (hasActiveDispute) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "该订单已有进行中的纠纷");
        }
        OrderActorRole actorRole = resolveActorRole(order, request.actorId());
        OrderDispute dispute = OrderDispute.create(
                actorRole,
                request.actorId(),
                request.option(),
                request.reason().trim(),
                StringUtils.hasText(request.remark()) ? request.remark().trim() : null
        );
        List<UUID> attachments = sanitizeAttachments(order, request.actorId(), request.attachmentProofIds());
        if (!attachments.isEmpty()) {
            dispute.setInitiatorAttachments(attachments);
        }
        String phoneMemo = StringUtils.hasText(request.phoneMemo()) ? request.phoneMemo().trim() : null;
        if (phoneMemo != null) {
            dispute.setInitiatorPhoneMemo(phoneMemo);
        }
        order.addDispute(dispute);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("option", request.option().name());
        attributes.put("reason", request.reason());
        if (!attachments.isEmpty()) {
            attributes.put("attachmentCount", attachments.size());
        }
        if (phoneMemo != null) {
            attributes.put("phoneMemo", phoneMemo);
        }
        timelineService.append(order,
                OrderEventType.DISPUTE_OPENED,
                "[%s] %s".formatted(request.option().name(), request.reason()),
                request.actorId(),
                attributes,
                actorRole);
        notifyCounterparty(order,
            dispute,
            actorRole,
                "订单纠纷已创建",
                "订单 %s 发起纠纷：%s".formatted(order.getOrderNo(), request.reason()));
        return orderAssembler.toDisputeResponse(dispute);
    }

    public OrderDisputeResponse respond(UUID orderId, UUID disputeId, OrderDisputeResponseRequest request) {
        RentalOrder order = loadOrder(orderId);
        OrderDispute dispute = loadDispute(orderId, disputeId);
        OrderActorRole actorRole = resolveActorRole(order, request.actorId());
        OrderActorRole lastResponder = dispute.getRespondentRole();
        if (lastResponder == null) {
            if (actorRole == dispute.getInitiatorRole()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "请等待对方回应");
            }
        } else if (lastResponder == actorRole) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请等待对方回应");
        }
        if (dispute.getStatus() != OrderDisputeStatus.OPEN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "当前纠纷状态不支持直接响应");
        }
        List<UUID> attachments = sanitizeAttachments(order, request.actorId(), request.attachmentProofIds());
        String phoneMemo = StringUtils.hasText(request.phoneMemo()) ? request.phoneMemo().trim() : null;
        dispute.recordResponse(
                actorRole,
                request.actorId(),
                request.option(),
                StringUtils.hasText(request.remark()) ? request.remark().trim() : null,
                request.accept()
        );
        if (!attachments.isEmpty()) {
            if (Objects.equals(dispute.getInitiatorId(), request.actorId())) {
                dispute.setInitiatorAttachments(attachments);
            } else {
                dispute.setRespondentAttachments(attachments);
            }
        }
        if (phoneMemo != null) {
            if (Objects.equals(dispute.getInitiatorId(), request.actorId())) {
                dispute.setInitiatorPhoneMemo(phoneMemo);
            } else {
                dispute.setRespondentPhoneMemo(phoneMemo);
            }
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("option", request.option().name());
        attributes.put("accept", request.accept());
        if (!attachments.isEmpty()) {
            attributes.put("attachmentCount", attachments.size());
        }
        if (phoneMemo != null) {
            attributes.put("phoneMemo", phoneMemo);
        }
        timelineService.append(order,
                OrderEventType.DISPUTE_RESPONDED,
                request.accept() ? "对方接受方案" : "对方建议方案：" + request.option(),
                request.actorId(),
                attributes,
                actorRole);
        notifyCounterparty(order,
            dispute,
            actorRole,
                request.accept() ? "纠纷达成一致" : "对方更新纠纷建议",
                request.accept()
                        ? "订单 %s 的纠纷达成一致，请留意后续处理。".formatted(order.getOrderNo())
                        : "订单 %s 对方建议方案 %s，请及时处理。"
                                .formatted(order.getOrderNo(), request.option().name()));
        if (request.accept()) {
            creditRewardService.rewardFriendlyDispute(order, dispute);
            orderSurveyService.scheduleForDispute(order, dispute);
            notifyResolutionTemplate(order, "协商达成一致", dispute.getId());
        }
        return orderAssembler.toDisputeResponse(dispute);
    }

    public OrderDisputeResponse escalate(UUID orderId, UUID disputeId, OrderDisputeEscalateRequest request) {
        RentalOrder order = loadOrder(orderId);
        OrderDispute dispute = loadDispute(orderId, disputeId);
        OrderActorRole actorRole = resolveActorRole(order, request.actorId());
        if (dispute.getStatus() == OrderDisputeStatus.CLOSED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "纠纷已结案");
        }
        dispute.escalate(request.actorId());
        String reason = StringUtils.hasText(request.reason()) ? request.reason().trim() : "请求平台介入";
        timelineService.append(order,
                OrderEventType.DISPUTE_ESCALATED,
                reason,
                request.actorId(),
                Map.of("reason", reason),
                actorRole);
        notifyUser(order,
            dispute,
            "纠纷进入仲裁",
            "订单 %s 已提交平台仲裁，平台将尽快处理。".formatted(order.getOrderNo()));
        notifyVendor(order,
            dispute,
            "纠纷进入仲裁",
            "订单 %s 已提交平台仲裁，请配合完成取证。".formatted(order.getOrderNo()));
        return orderAssembler.toDisputeResponse(dispute);
    }

    public OrderDisputeResponse appeal(UUID orderId, UUID disputeId, OrderDisputeAppealRequest request) {
        RentalOrder order = loadOrder(orderId);
        OrderDispute dispute = loadDispute(orderId, disputeId);
        OrderActorRole actorRole = resolveActorRole(order, request.actorId());
        if (dispute.getStatus() != OrderDisputeStatus.CLOSED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅已结案的纠纷可申诉");
        }
        dispute.appeal(request.actorId());
        String reason = StringUtils.hasText(request.reason()) ? request.reason().trim() : "申诉请求";
        timelineService.append(order,
                OrderEventType.DISPUTE_ESCALATED,
                "申诉（转复核组）：" + reason,
                request.actorId(),
                Map.of("reason", reason, "appealCount", dispute.getAppealCount(), "reviewPanel", true),
                actorRole);
        notifyUser(order,
            dispute,
            "纠纷申诉已受理",
            "订单 %s 已发起申诉，平台复核组将重新审理。".formatted(order.getOrderNo()));
        notifyVendor(order,
            dispute,
            "纠纷申诉已受理",
            "订单 %s 已进入复核组审理阶段，请关注处理结果。".formatted(order.getOrderNo()));
        return orderAssembler.toDisputeResponse(dispute);
    }


    public OrderDisputeResponse resolve(UUID orderId, UUID disputeId, OrderDisputeResolveRequest request) {
        // 复核组权限校验：二次申诉案件需要 REVIEW_PANEL/ARBITRATOR 角色
        RentalOrder order = loadOrder(orderId);
        OrderDispute dispute = loadDispute(orderId, disputeId);
        if (dispute.getStatus() == OrderDisputeStatus.PENDING_REVIEW_PANEL) {
            SecurityUtils.requireAnyRole("REVIEW_PANEL", "ARBITRATOR");
        } else {
            SecurityUtils.requireRole("ARBITRATOR");
        }
        UUID operatorId = SecurityUtils.requireUserId();
        Integer normalizedDelta = normalizeCreditDelta(request.penalizeUserDelta());
        boolean isMalicious = Boolean.TRUE.equals(request.maliciousBehavior());
        dispute.resolveByAdmin(
            request.decision(),
            StringUtils.hasText(request.remark()) ? request.remark().trim() : null,
            operatorId,
            normalizedDelta
        );
        Map<String, Object> resolutionAttributes = new java.util.HashMap<>();
        resolutionAttributes.put("decision", request.decision().name());
        if (normalizedDelta != null) {
            resolutionAttributes.put("penalizeUserDelta", normalizedDelta);
        }
        if (isMalicious) {
            resolutionAttributes.put("maliciousBehavior", true);
        }
        timelineService.append(order,
                OrderEventType.DISPUTE_RESOLVED,
                buildDecisionMessage(request, normalizedDelta, isMalicious),
                operatorId,
                resolutionAttributes,
                resolvePlatformActorRole());

        // 处理恶意行为：扣 30 分并冻结账号 30 天
        if (isMalicious) {
            String reason = StringUtils.hasText(request.remark()) ? request.remark().trim() : "恶意行为";
            creditRewardService.penalizeMaliciousBehavior(order, reason);
        } else if (normalizedDelta != null && normalizedDelta != 0) {
            // 普通信用调整
            try {
                userProfileClient.adjustCredit(order.getUserId(),
                        normalizedDelta,
                        "订单纠纷裁决");
                notifyCreditChange(order, normalizedDelta);
            } catch (RuntimeException ex) {
                LOG.warn("Failed to adjust credit for user {} due to dispute {}: {}",
                        order.getUserId(), disputeId, ex.getMessage());
            }
        }
        String summary = buildDecisionMessage(request, normalizedDelta, isMalicious);
        notifyUser(order,
            dispute,
            "纠纷裁决结果",
            "订单 %s 纠纷裁决：%s".formatted(order.getOrderNo(), summary));
        notifyVendor(order,
            dispute,
            "纠纷裁决结果",
            "订单 %s 纠纷裁决：%s".formatted(order.getOrderNo(), summary));
        notifyResolutionTemplate(order, summary, dispute.getId());
        orderSurveyService.scheduleForDispute(order, dispute);
        return orderAssembler.toDisputeResponse(dispute);
    }

    private OrderActorRole resolvePlatformActorRole() {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("REVIEW_PANEL")) {
            return OrderActorRole.REVIEW_PANEL;
        }
        return OrderActorRole.ARBITRATOR;
    }


    public boolean escalateDisputeDueToTimeout(UUID disputeId) {
        OrderDispute dispute = orderDisputeRepository.findById(disputeId)
                .orElse(null);
        if (dispute == null) {
            return false;
        }
        if (dispute.getStatus() != OrderDisputeStatus.OPEN) {
            return false;
        }
        OffsetDateTime deadline = dispute.getDeadlineAt();
        if (deadline == null || deadline.isAfter(OffsetDateTime.now())) {
            return false;
        }
        RentalOrder order = dispute.getOrder();
        dispute.escalate(null);
        String reason = "协商超时，系统自动升级平台仲裁";
        timelineService.append(order,
                OrderEventType.DISPUTE_ESCALATED,
                reason,
                null,
                Map.of("reason", reason, "auto", true),
                OrderActorRole.INTERNAL);
        notifyUser(order,
            dispute,
            "纠纷自动进入仲裁",
            "订单 %s 因协商超时已自动升级平台仲裁，平台将尽快处理。".formatted(order.getOrderNo()));
        notifyVendor(order,
            dispute,
            "纠纷自动进入仲裁",
            "订单 %s 因协商超时已自动升级平台仲裁，请关注仲裁通知。".formatted(order.getOrderNo()));
        return true;
    }

    public void sendCountdownReminder(UUID disputeId) {
        sendCountdownReminder(disputeId, 1, 24);
    }

    public boolean sendCountdownReminder(UUID disputeId, int targetLevel, int hoursLeft) {
        OrderDispute dispute = orderDisputeRepository.findById(disputeId).orElse(null);
        if (dispute == null || dispute.getStatus() != OrderDisputeStatus.OPEN || dispute.getDeadlineAt() == null) {
            return false;
        }
        if (!dispute.advanceReminderLevel(targetLevel)) {
            return false;
        }
        orderDisputeRepository.save(dispute);
        RentalOrder order = dispute.getOrder();
        Map<String, Object> variables = Map.of(
                "orderNo", order.getOrderNo(),
                "hoursLeft", hoursLeft
        );
        sendTemplateNotification(order.getUserId(), "DISPUTE_COUNTDOWN", variables, dispute.getId().toString());
        sendTemplateNotification(order.getVendorId(), "DISPUTE_COUNTDOWN", variables, dispute.getId().toString());
        return true;
    }

    private String buildDecisionMessage(OrderDisputeResolveRequest request, Integer normalizedDelta, boolean malicious) {
        String decision = switch (request.decision()) {
            case REDELIVER -> "重新发货/补发";
            case PARTIAL_REFUND -> "部分退款继续租赁";
            case RETURN_WITH_DEPOSIT_DEDUCTION -> "退租并扣押金";
            case DISCOUNTED_BUYOUT -> "优惠买断";
            case CUSTOM -> "自定义方案";
        };
        if (malicious) {
            return "%s，判定恶意行为，信用扣 30 分并冻结账号 30 天".formatted(decision);
        }
        if (normalizedDelta != null && normalizedDelta != 0) {
            return "%s，信用变动 %d 分".formatted(decision, normalizedDelta);
        }
        return decision;
    }

    private List<UUID> sanitizeAttachments(RentalOrder order, UUID actorId, List<UUID> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        if (attachmentIds.size() > MAX_ATTACHMENTS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "附件数量超出上限");
        }
        List<UUID> sanitized = new ArrayList<>(attachmentIds.size());
        for (UUID attachmentId : attachmentIds) {
            if (attachmentId == null) {
                continue;
            }
            OrderProof proof = orderProofRepository.findByIdAndOrderId(attachmentId, order.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "附件不存在或不属于该订单"));
            if (actorId != null && proof.getUploadedBy() != null && !proof.getUploadedBy().equals(actorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅可引用本人上传的附件");
            }
            sanitized.add(proof.getId());
        }
        return sanitized;
    }


    private Integer normalizeCreditDelta(Integer rawDelta) {
        if (rawDelta == null) {
            return null;
        }
        if (rawDelta == 0) {
            return 0;
        }
        if (rawDelta > 0) {
            return -rawDelta;
        }
        return Math.abs(rawDelta);
    }

    private void notifyCreditChange(RentalOrder order, int delta) {
        String direction = delta > 0 ? "增加" : "扣减";
        int absolute = Math.abs(delta);
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                order.getUserId().toString(),
                "信用积分变动提醒",
                "订单 %s 纠纷裁决导致信用%s %d 分。".formatted(order.getOrderNo(), direction, absolute),
                Map.of(
                        "orderNo", order.getOrderNo(),
                        "creditDelta", delta
                ),
                CREDIT_CONTEXT,
                order.getId().toString()
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to notify user {} of credit change: {}", order.getUserId(), ex.getMessage());
        }
    }

    private void notifyResolutionTemplate(RentalOrder order, String summary, UUID disputeId) {
        Map<String, Object> vars = Map.of(
                "orderNo", order.getOrderNo(),
                "result", summary
        );
        sendTemplateNotification(order.getUserId(), "DISPUTE_RESOLVED", vars, disputeId.toString());
        sendTemplateNotification(order.getVendorId(), "DISPUTE_RESOLVED", vars, disputeId.toString());
    }

    private void sendTemplateNotification(UUID recipient,
                                          String templateCode,
                                          Map<String, Object> variables,
                                          String reference) {
        NotificationSendRequest request = new NotificationSendRequest(
                templateCode,
                recipient.toString(),
                null,
                null,
                variables,
                DISPUTE_CONTEXT,
                reference
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to send template {} for recipient {}: {}", templateCode, recipient, ex.getMessage());
        }
    }

    private RentalOrder loadOrder(UUID orderId) {
        return rentalOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    private OrderDispute loadDispute(UUID orderId, UUID disputeId) {
        return orderDisputeRepository.findByIdAndOrderId(disputeId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "纠纷不存在"));
    }

    private void ensureReadable(RentalOrder order) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN")
                || principal.hasRole("ARBITRATOR")
                || principal.hasRole("REVIEW_PANEL")
                || principal.hasRole("INTERNAL")) {
            return;
        }
        if (principal.hasRole("VENDOR")) {
            UUID vendorId = principal.vendorId();
            if (vendorId == null || !vendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单纠纷");
            }
            return;
        }
        if (principal.hasRole("USER")) {
            UUID userId = principal.userId();
            if (userId == null || !userId.equals(order.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单纠纷");
            }
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少访问权限");
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
        if (principal.hasRole("ARBITRATOR")) {
            return OrderActorRole.ARBITRATOR;
        }
        if (principal.hasRole("REVIEW_PANEL")) {
            return OrderActorRole.REVIEW_PANEL;
        }
        if (principal.hasRole("INTERNAL")) {
            return OrderActorRole.INTERNAL;
        }
        if (principal.hasRole("VENDOR")) {
            UUID vendorId = principal.vendorId();
            if (vendorId == null || !vendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权处理该订单");
            }
            return OrderActorRole.VENDOR;
        }
        if (principal.hasRole("USER")) {
            if (!order.getUserId().equals(actorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权处理该订单");
            }
            return OrderActorRole.USER;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少处理纠纷的权限");
    }

    private void notifyCounterparty(RentalOrder order,
                                    OrderDispute dispute,
                                    OrderActorRole actorRole,
                                    String subject,
                                    String content) {
        if (actorRole == OrderActorRole.USER) {
            notifyVendor(order, dispute, subject, content);
        } else if (actorRole == OrderActorRole.VENDOR) {
            notifyUser(order, dispute, subject, content);
        } else {
            notifyUser(order, dispute, subject, content);
            notifyVendor(order, dispute, subject, content);
        }
    }

    private void notifyUser(RentalOrder order, OrderDispute dispute, String subject, String content) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                order.getUserId().toString(),
                subject,
                content,
                Map.of("orderNo", order.getOrderNo()),
                dispute != null ? DISPUTE_CONTEXT : null,
                dispute != null ? dispute.getId().toString() : null
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("发送用户纠纷通知失败: {}", ex.getMessage());
        }
    }

    private void notifyVendor(RentalOrder order, OrderDispute dispute, String subject, String content) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                order.getVendorId().toString(),
                subject,
                content,
                Map.of("orderNo", order.getOrderNo()),
                dispute != null ? DISPUTE_CONTEXT : null,
                dispute != null ? dispute.getId().toString() : null
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("发送厂商纠纷通知失败: {}", ex.getMessage());
        }
    }
}
