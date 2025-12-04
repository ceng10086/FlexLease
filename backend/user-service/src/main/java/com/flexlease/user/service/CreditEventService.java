package com.flexlease.user.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.user.domain.CreditEventType;
import com.flexlease.user.domain.UserProfile;
import com.flexlease.user.dto.UserCreditResponse;
import com.flexlease.user.integration.NotificationClient;
import com.flexlease.user.repository.UserProfileRepository;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CreditEventService {

    private static final Logger LOG = LoggerFactory.getLogger(CreditEventService.class);
    private static final int STREAK_WINDOW = 3;

    private final UserProfileRepository userProfileRepository;
    private final NotificationClient notificationClient;

    public CreditEventService(UserProfileRepository userProfileRepository,
                              NotificationClient notificationClient) {
        this.userProfileRepository = userProfileRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public UserCreditResponse applyEvent(UUID userId,
                                         CreditEventType eventType,
                                         Map<String, Object> attributes) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.create(userId)));
        switch (eventType) {
            case KYC_VERIFIED -> handleKyc(profile, attributes);
            case ON_TIME_PAYMENT -> handleOnTimePayment(profile, attributes);
            case EARLY_RETURN -> handleEarlyReturn(profile, attributes);
            case LATE_PAYMENT -> handleLatePayment(profile, attributes);
            case FRIENDLY_DISPUTE -> handleFriendlyDispute(profile, attributes);
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的信用事件: " + eventType);
        }
        UserProfile saved = userProfileRepository.save(profile);
        return new UserCreditResponse(saved.getUserId(), saved.getCreditScore(), saved.getCreditTier());
    }

    private void handleKyc(UserProfile profile, Map<String, Object> attributes) {
        boolean verified = profile.markKycVerified();
        if (!verified) {
            LOG.debug("User {} already verified KYC, skip bonus", profile.getUserId());
            return;
        }
        profile.applyCreditDelta(10);
        notifyUser(profile.getUserId(),
                "实名审核通过",
                "实名认证完成，信用积分 +10，押金减免自动生效。",
                "CREDIT");
    }

    private void handleOnTimePayment(UserProfile profile, Map<String, Object> attributes) {
        profile.applyCreditDelta(5);
        int streak = profile.incrementPaymentStreak();
        String orderNo = attributeAsString(attributes, "orderNo");
        notifyUser(profile.getUserId(),
                "按时支付奖励",
                buildOrderContext(orderNo, "按时完成支付，信用积分 +5。"),
                "CREDIT");
        if (profile.advancePaymentMilestoneIfNeeded(STREAK_WINDOW)) {
            notifyUser(profile.getUserId(),
                    "稳定履约加成",
                    "连续 " + STREAK_WINDOW + " 单准时支付，平台已为你解锁“快速审核”通道。",
                    "CREDIT");
        }
    }

    private void handleEarlyReturn(UserProfile profile, Map<String, Object> attributes) {
        profile.applyCreditDelta(8);
        String orderNo = attributeAsString(attributes, "orderNo");
        notifyUser(profile.getUserId(),
                "按约归还奖励",
                buildOrderContext(orderNo, "按约归还已完成，信用积分 +8，押金将在 12 小时内退还。"),
                "CREDIT");
    }

    private void handleLatePayment(UserProfile profile, Map<String, Object> attributes) {
        profile.applyCreditDelta(-8);
        profile.resetPaymentStreak();
        String orderNo = attributeAsString(attributes, "orderNo");
        notifyUser(profile.getUserId(),
                "支付超时提醒",
                buildOrderContext(orderNo, "支付超时被系统取消，信用积分 -8，请下单时关注倒计时。"),
                "ALERT");
    }

    private void handleFriendlyDispute(UserProfile profile, Map<String, Object> attributes) {
        profile.applyCreditDelta(3);
        String orderNo = attributeAsString(attributes, "orderNo");
        notifyUser(profile.getUserId(),
                "友好协商奖励",
                buildOrderContext(orderNo, "纠纷友好协商达成一致，信用积分 +3。"),
                "DISPUTE");
    }

    private void notifyUser(UUID userId,
                            String subject,
                            String content,
                            String contextType) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                userId.toString(),
                subject,
                content,
                Map.of(),
                contextType,
                null
        );
        notificationClient.send(request);
    }

    private String attributeAsString(Map<String, Object> attributes, String key) {
        if (attributes == null || key == null) {
            return null;
        }
        Object value = attributes.get(key);
        return value instanceof String str && StringUtils.hasText(str) ? str : null;
    }

    private String buildOrderContext(String orderNo, String message) {
        if (!StringUtils.hasText(orderNo)) {
            return message;
        }
        return "订单 " + orderNo + " " + message;
    }
}
