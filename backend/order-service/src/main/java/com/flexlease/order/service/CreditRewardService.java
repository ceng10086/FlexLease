package com.flexlease.order.service;

import com.flexlease.order.client.UserProfileClient;
import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.RentalOrder;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 信用奖惩服务：把订单关键行为映射为 user-service 的信用事件。
 * <p>
 * 该能力属于“尽力而为”的联动：若 user-service 暂时不可用，会记录告警但不阻塞主交易流程。
 */
@Service
public class CreditRewardService {

    private static final Logger LOG = LoggerFactory.getLogger(CreditRewardService.class);
    private static final Duration EARLY_RETURN_GRACE = Duration.ofHours(24);

    private final UserProfileClient userProfileClient;

    public CreditRewardService(UserProfileClient userProfileClient) {
        this.userProfileClient = userProfileClient;
    }

    public void rewardOnTimePayment(RentalOrder order) {
        try {
            userProfileClient.recordCreditEvent(order.getUserId(),
                    "ON_TIME_PAYMENT",
                    Map.of("orderNo", order.getOrderNo()));
        } catch (RuntimeException ex) {
            LOG.warn("Failed to record on-time payment credit event for order {}: {}", order.getOrderNo(), ex.getMessage());
        }
    }

    public void rewardEarlyReturn(RentalOrder order, OffsetDateTime plannedLeaseEnd) {
        if (!isEarlyOrOnTime(plannedLeaseEnd)) {
            return;
        }
        try {
            userProfileClient.recordCreditEvent(order.getUserId(),
                    "EARLY_RETURN",
                    Map.of("orderNo", order.getOrderNo()));
        } catch (RuntimeException ex) {
            LOG.warn("Failed to record early return credit event for order {}: {}", order.getOrderNo(), ex.getMessage());
        }
    }

    public void penalizeLatePayment(RentalOrder order) {
        try {
            userProfileClient.recordCreditEvent(order.getUserId(),
                    "LATE_PAYMENT",
                    Map.of("orderNo", order.getOrderNo()));
        } catch (RuntimeException ex) {
            LOG.warn("Failed to record late payment credit event for order {}: {}", order.getOrderNo(), ex.getMessage());
        }
    }

    public void rewardFriendlyDispute(RentalOrder order, OrderDispute dispute) {
        try {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("orderNo", order.getOrderNo());
            attributes.put("disputeId", dispute.getId().toString());
            userProfileClient.recordCreditEvent(order.getUserId(), "FRIENDLY_DISPUTE", attributes);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to record friendly dispute credit event for order {}: {}", order.getOrderNo(), ex.getMessage());
        }
    }

    /**
     * 恶意行为处罚：扣 30 分并冻结账号 30 天。
     * 适用于恶意拒收、拒不退还、经判定需赔偿等严重违规行为。
     */
    public void penalizeMaliciousBehavior(RentalOrder order, String reason) {
        try {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("orderNo", order.getOrderNo());
            if (reason != null && !reason.isBlank()) {
                attributes.put("reason", reason);
            }
            userProfileClient.recordCreditEvent(order.getUserId(), "MALICIOUS_BEHAVIOR", attributes);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to record malicious behavior credit event for order {}: {}", order.getOrderNo(), ex.getMessage());
        }
    }

    public void rewardInspectionCooperation(RentalOrder order) {
        try {
            userProfileClient.recordCreditEvent(order.getUserId(),
                    "INSPECTION_COOPERATED",
                    Map.of("orderNo", order.getOrderNo()));
        } catch (RuntimeException ex) {
            LOG.warn("Failed to record inspection cooperation credit event for order {}: {}", order.getOrderNo(), ex.getMessage());
        }
    }

    private boolean isEarlyOrOnTime(OffsetDateTime plannedLeaseEnd) {
        if (plannedLeaseEnd == null) {
            return true;
        }
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isAfter(plannedLeaseEnd.plus(EARLY_RETURN_GRACE));
    }
}
