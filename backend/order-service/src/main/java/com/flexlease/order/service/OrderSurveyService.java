package com.flexlease.order.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.config.OrderSurveyProperties;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderSatisfactionSurvey;
import com.flexlease.order.domain.OrderSurveyStatus;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.dto.OrderSurveyResponse;
import com.flexlease.order.dto.OrderSurveySubmitRequest;
import com.flexlease.order.repository.OrderSatisfactionSurveyRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.client.NotificationClient;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class OrderSurveyService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderSurveyService.class);
    private static final EnumSet<OrderSurveyStatus> ACTIVE_SURVEY_STATUSES =
            EnumSet.of(OrderSurveyStatus.PENDING, OrderSurveyStatus.OPEN);

    private final RentalOrderRepository rentalOrderRepository;
    private final OrderSatisfactionSurveyRepository surveyRepository;
    private final OrderAssembler orderAssembler;
    private final OrderTimelineService timelineService;
    private final NotificationClient notificationClient;
    private final OrderSurveyProperties surveyProperties;

    public OrderSurveyService(RentalOrderRepository rentalOrderRepository,
                              OrderSatisfactionSurveyRepository surveyRepository,
                              OrderAssembler orderAssembler,
                              OrderTimelineService timelineService,
                              NotificationClient notificationClient,
                              OrderSurveyProperties surveyProperties) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.surveyRepository = surveyRepository;
        this.orderAssembler = orderAssembler;
        this.timelineService = timelineService;
        this.notificationClient = notificationClient;
        this.surveyProperties = surveyProperties;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<OrderSurveyResponse> list(UUID orderId) {
        RentalOrder order = loadOrder(orderId);
        ensureReadable(order);
        return order.getSurveys().stream()
                .map(orderAssembler::toSurveyResponse)
                .toList();
    }

    public OrderSurveyResponse submit(UUID orderId, UUID surveyId, OrderSurveySubmitRequest request) {
        RentalOrder order = loadOrder(orderId);
        OrderSatisfactionSurvey survey = surveyRepository.findByIdAndOrderId(surveyId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "满意度调查不存在"));
        OrderActorRole actorRole = resolveActorRole(order, request.actorId());
        if (survey.getTargetRole() != actorRole) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前调查不属于该角色");
        }
        if (survey.getStatus() != OrderSurveyStatus.OPEN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "调查尚未开放或已完成");
        }
        String comment = StringUtils.hasText(request.comment()) ? request.comment().trim() : null;
        survey.markCompleted(request.rating(), comment);
        timelineService.append(order,
                OrderEventType.SURVEY_SUBMITTED,
                "完成满意度调查（" + request.rating() + " 分）",
                request.actorId(),
                Map.of("rating", request.rating()),
                actorRole);
        sendThankYouNotification(order, survey, request.rating());
        return orderAssembler.toSurveyResponse(survey);
    }

    public void scheduleForDispute(RentalOrder order, OrderDispute dispute) {
        if (dispute.getId() == null) {
            return;
        }
        int delayHours = Math.max(0, surveyProperties.getReminderDelayHours());
        OffsetDateTime availableAt = OffsetDateTime.now().plusHours(delayHours);
        scheduleIfNeeded(order, dispute, OrderActorRole.USER, order.getUserId(), availableAt);
        scheduleIfNeeded(order, dispute, OrderActorRole.VENDOR, order.getVendorId(), availableAt);
    }

    public void activatePendingSurveys() {
        int batchSize = Math.max(1, surveyProperties.getActivationBatchSize());
        List<OrderSatisfactionSurvey> pending = surveyRepository.findReadyForActivation(
                OrderSurveyStatus.PENDING,
                OffsetDateTime.now(),
                PageRequest.of(0, batchSize)
        );
        for (OrderSatisfactionSurvey survey : pending) {
            survey.markOpen();
            timelineService.append(
                    survey.getOrder(),
                    OrderEventType.SURVEY_INVITED,
                    buildInviteMessage(survey.getTargetRole(), survey.getAvailableAt()),
                    null,
                    Map.of("targetRole", survey.getTargetRole().name(),
                            "availableAt", survey.getAvailableAt().toString()),
                    OrderActorRole.INTERNAL
            );
            sendSurveyInvite(survey);
        }
    }

    private void scheduleIfNeeded(RentalOrder order,
                                  OrderDispute dispute,
                                  OrderActorRole targetRole,
                                  UUID targetRef,
                                  OffsetDateTime availableAt) {
        if (targetRef == null) {
            return;
        }
        boolean hasActive = surveyRepository.existsByDisputeIdAndTargetRoleAndTargetRefAndStatusIn(
                dispute.getId(),
                targetRole,
                targetRef,
                ACTIVE_SURVEY_STATUSES
        );
        if (hasActive) {
            return;
        }
        OrderSatisfactionSurvey survey = OrderSatisfactionSurvey.create(targetRole, targetRef, availableAt);
        survey.setDispute(dispute);
        order.addSurvey(survey);
        timelineService.append(order,
                OrderEventType.SURVEY_INVITED,
                "已安排满意度调查，将于 %s 开放".formatted(availableAt),
                null,
                Map.of("targetRole", targetRole.name(),
                        "availableAt", availableAt.toString()),
                OrderActorRole.INTERNAL);
    }

    private RentalOrder loadOrder(UUID orderId) {
        return rentalOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    private void ensureReadable(RentalOrder order) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        if (principal.hasRole("VENDOR")) {
            UUID vendorId = principal.vendorId();
            if (vendorId == null || !vendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该订单调查");
            }
            return;
        }
        if (principal.hasRole("USER")) {
            UUID userId = principal.userId();
            if (userId == null || !userId.equals(order.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该订单调查");
            }
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少访问权限");
    }

    private OrderActorRole resolveActorRole(RentalOrder order, UUID actorId) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        UUID currentUserId = principal.userId();
        if (currentUserId == null || !currentUserId.equals(actorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请求用户和当前登录用户不一致");
        }
        if (principal.hasRole("USER")) {
            if (!order.getUserId().equals(actorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权处理该订单");
            }
            return OrderActorRole.USER;
        }
        if (principal.hasRole("VENDOR")) {
            UUID vendorId = principal.vendorId();
            if (vendorId == null || !vendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权处理该订单");
            }
            return OrderActorRole.VENDOR;
        }
        if (principal.hasRole("ADMIN")) {
            return OrderActorRole.ADMIN;
        }
        if (principal.hasRole("INTERNAL")) {
            return OrderActorRole.INTERNAL;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少操作权限");
    }

    private void sendSurveyInvite(OrderSatisfactionSurvey survey) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                survey.getTargetRef().toString(),
                "满意度调查邀请",
                buildInviteMessage(survey.getTargetRole(), survey.getAvailableAt()),
                Map.of("orderId", survey.getOrder().getId().toString())
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("发送满意度邀请失败: {}", ex.getMessage());
        }
    }

    private void sendThankYouNotification(RentalOrder order, OrderSatisfactionSurvey survey, int rating) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                survey.getTargetRef().toString(),
                "感谢您的反馈",
                "感谢对订单 %s 的满意度评分：%d 分。".formatted(order.getOrderNo(), rating),
                Map.of("orderNo", order.getOrderNo())
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("发送满意度感谢失败: {}", ex.getMessage());
        }
    }

    private String buildInviteMessage(OrderActorRole role, OffsetDateTime availableAt) {
        return switch (role) {
            case USER -> "订单满意度调查已开放，感谢提供建议（开放时间：" + availableAt + "）";
            case VENDOR -> "请对近期纠纷处理给出反馈，开放时间：" + availableAt;
            default -> "满意度调查即将开启";
        };
    }
}
