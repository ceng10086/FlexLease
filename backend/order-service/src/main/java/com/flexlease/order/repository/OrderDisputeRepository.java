package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.OrderDisputeStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderDisputeRepository extends JpaRepository<OrderDispute, UUID> {

    Optional<OrderDispute> findByIdAndOrderId(UUID id, UUID orderId);

    long countByStatus(OrderDisputeStatus status);

    @Query("""
            select count(d) from OrderDispute d
            where d.order.vendorId = :vendorId and d.status = :status
            """)
    long countByVendorAndStatus(@Param("vendorId") UUID vendorId, @Param("status") OrderDisputeStatus status);

    @Query("""
            select d.createdAt as createdAt,
                   coalesce(d.adminDecisionAt, d.respondedAt, d.updatedAt) as resolvedAt
            from OrderDispute d
            where d.status in :statuses
            """)
    List<ResolutionMetric> findResolutionMetrics(@Param("statuses") Collection<OrderDisputeStatus> statuses);

    @Query("""
            select d.createdAt as createdAt,
                   coalesce(d.adminDecisionAt, d.respondedAt, d.updatedAt) as resolvedAt
            from OrderDispute d
            where d.status in :statuses and d.order.vendorId = :vendorId
            """)
    List<ResolutionMetric> findResolutionMetricsByVendor(@Param("vendorId") UUID vendorId,
                                                         @Param("statuses") Collection<OrderDisputeStatus> statuses);

    @Query("""
            select d.id from OrderDispute d
            where d.status = :status
              and d.deadlineAt is not null
              and d.deadlineAt <= :deadline
            """)
    List<UUID> findIdsByStatusAndDeadlineAtBefore(@Param("status") OrderDisputeStatus status,
                                                  @Param("deadline") OffsetDateTime deadline);

    @Query("""
            select d.id from OrderDispute d
            where d.status = :status
              and d.deadlineAt between :from and :to
              and d.countdownNotifiedAt is null
            """)
    List<UUID> findIdsByStatusAndDeadlineBetween(@Param("status") OrderDisputeStatus status,
                                                 @Param("from") OffsetDateTime from,
                                                 @Param("to") OffsetDateTime to);

    /**
     * 查找需要发送指定级别提醒的纠纷。
     * @param status 纠纷状态
     * @param from 截止时间起点
     * @param to 截止时间终点
     * @param reminderLevel 目标提醒级别（未达到该级别的纠纷将被返回）
     */
    @Query("""
            select d.id from OrderDispute d
            where d.status = :status
              and d.deadlineAt between :from and :to
              and d.countdownReminderLevel < :reminderLevel
            """)
    List<UUID> findIdsByStatusAndDeadlineBetweenAndReminderLevelLessThan(
            @Param("status") OrderDisputeStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("reminderLevel") int reminderLevel);

    interface ResolutionMetric {
        OffsetDateTime getCreatedAt();

        OffsetDateTime getResolvedAt();
    }
}
