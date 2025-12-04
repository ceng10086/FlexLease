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

/**
 * 纠纷维护调度器。
 * <p>
 * 功能：
 * <ol>
 *   <li>自动升级超时纠纷</li>
 *   <li>发送多阶段倒计时提醒（24 小时、6 小时、1 小时）</li>
 * </ol>
 */
@Service
public class OrderDisputeMaintenanceScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(OrderDisputeMaintenanceScheduler.class);

    // 多阶段提醒配置：级别 -> (时间窗口, 小时数)
    private static final int REMINDER_LEVEL_24H = 1;
    private static final int REMINDER_LEVEL_6H = 2;
    private static final int REMINDER_LEVEL_1H = 3;

    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final Duration WINDOW_6H = Duration.ofHours(6);
    private static final Duration WINDOW_1H = Duration.ofHours(1);

    private final OrderDisputeRepository orderDisputeRepository;
    private final OrderDisputeService orderDisputeService;

    public OrderDisputeMaintenanceScheduler(OrderDisputeRepository orderDisputeRepository,
                                            OrderDisputeService orderDisputeService) {
        this.orderDisputeRepository = orderDisputeRepository;
        this.orderDisputeService = orderDisputeService;
    }

    @Scheduled(fixedDelayString = "${flexlease.order.dispute.scan-interval-ms:300000}")
    public void escalateOverdueDisputes() {
        sendMultiStageReminders();
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

    /**
     * 发送多阶段倒计时提醒。
     * <ul>
     *   <li>24 小时前：第一次提醒</li>
     *   <li>6 小时前：第二次提醒</li>
     *   <li>1 小时前：最后提醒</li>
     * </ul>
     */
    private void sendMultiStageReminders() {
        OffsetDateTime now = OffsetDateTime.now();

        // 24 小时提醒
        sendRemindersForLevel(now, WINDOW_24H, REMINDER_LEVEL_24H, 24);

        // 6 小时提醒
        sendRemindersForLevel(now, WINDOW_6H, REMINDER_LEVEL_6H, 6);

        // 1 小时提醒
        sendRemindersForLevel(now, WINDOW_1H, REMINDER_LEVEL_1H, 1);
    }

    private void sendRemindersForLevel(OffsetDateTime now, Duration window, int level, int hoursLeft) {
        OffsetDateTime windowEnd = now.plus(window);
        List<UUID> ids = orderDisputeRepository.findIdsByStatusAndDeadlineBetweenAndReminderLevelLessThan(
                OrderDisputeStatus.OPEN,
                now,
                windowEnd,
                level
        );
        for (UUID id : ids) {
            boolean sent = orderDisputeService.sendCountdownReminder(id, level, hoursLeft);
            if (sent) {
                LOG.debug("Sent {}h countdown reminder for dispute {}", hoursLeft, id);
            }
        }
    }
}
