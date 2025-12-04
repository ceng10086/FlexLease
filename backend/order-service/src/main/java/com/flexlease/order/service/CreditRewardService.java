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

    private boolean isEarlyOrOnTime(OffsetDateTime plannedLeaseEnd) {
        if (plannedLeaseEnd == null) {
            return true;
        }
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isAfter(plannedLeaseEnd.plus(EARLY_RETURN_GRACE));
    }
}
