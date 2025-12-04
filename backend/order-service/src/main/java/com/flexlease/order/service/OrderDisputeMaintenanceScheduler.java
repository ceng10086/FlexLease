package com.flexlease.order.service;

import com.flexlease.order.domain.OrderDisputeStatus;
import com.flexlease.order.repository.OrderDisputeRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OrderDisputeMaintenanceScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(OrderDisputeMaintenanceScheduler.class);
    private static final Duration COUNTDOWN_WINDOW = Duration.ofHours(12);

    private final OrderDisputeRepository orderDisputeRepository;
    private final OrderDisputeService orderDisputeService;

    public OrderDisputeMaintenanceScheduler(OrderDisputeRepository orderDisputeRepository,
                                            OrderDisputeService orderDisputeService) {
        this.orderDisputeRepository = orderDisputeRepository;
        this.orderDisputeService = orderDisputeService;
    }

    @Scheduled(fixedDelayString = "${flexlease.order.dispute.scan-interval-ms:300000}")
    public void escalateOverdueDisputes() {
        sendCountdownReminders();
        List<UUID> overdueIds = orderDisputeRepository.findIdsByStatusAndDeadlineAtBefore(
                OrderDisputeStatus.OPEN,
                OffsetDateTime.now()
        );
        overdueIds.forEach(id -> {
            boolean escalated = orderDisputeService.escalateDisputeDueToTimeout(id);
            if (escalated) {
                LOG.info("Auto escalated dispute {} due to timeout", id);
            }
        });
    }

    private void sendCountdownReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowEnd = now.plus(COUNTDOWN_WINDOW);
        List<UUID> ids = orderDisputeRepository.findIdsByStatusAndDeadlineBetween(
                OrderDisputeStatus.OPEN,
                now,
                windowEnd
        );
        ids.forEach(orderDisputeService::sendCountdownReminder);
    }
}
