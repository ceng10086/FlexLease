package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.domain.RentalOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalOrderRepository extends JpaRepository<RentalOrder, UUID> {

    @EntityGraph(attributePaths = {"items"})
    @Query("select o from RentalOrder o where o.id = :id")
    Optional<RentalOrder> findByIdWithDetails(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"items"})
    List<RentalOrder> findByStatusAndCreatedAtBefore(OrderStatus status, OffsetDateTime createdAt);

    Page<RentalOrder> findByUserId(UUID userId, Pageable pageable);

    Page<RentalOrder> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Page<RentalOrder> findByVendorId(UUID vendorId, Pageable pageable);

    Page<RentalOrder> findByVendorIdAndStatus(UUID vendorId, OrderStatus status, Pageable pageable);

    Page<RentalOrder> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByStatusIn(Collection<OrderStatus> statuses);

    long countByVendorId(UUID vendorId);

    long countByVendorIdAndStatus(UUID vendorId, OrderStatus status);

    long countByVendorIdAndStatusIn(UUID vendorId, Collection<OrderStatus> statuses);

    @Query("select o.status as status, count(o) as count from RentalOrder o group by o.status")
    List<OrderStatusCount> aggregateStatus();

    @Query("select o.status as status, count(o) as count from RentalOrder o where o.vendorId = :vendorId group by o.status")
    List<OrderStatusCount> aggregateStatusByVendor(@Param("vendorId") UUID vendorId);

    @Query("select coalesce(sum(o.totalAmount), 0) from RentalOrder o where o.status in :statuses")
    BigDecimal sumTotalAmountByStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("select coalesce(sum(o.totalAmount), 0) from RentalOrder o where o.vendorId = :vendorId and o.status in :statuses")
    BigDecimal sumTotalAmountByVendorIdAndStatusIn(@Param("vendorId") UUID vendorId,
                                                   @Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            select cast(coalesce(o.leaseStartAt, o.createdAt) as date) as day,
                   count(o) as orderCount,
                   coalesce(sum(o.totalAmount), 0) as totalAmount
            from RentalOrder o
            where coalesce(o.leaseStartAt, o.createdAt) between :start and :end
            group by cast(coalesce(o.leaseStartAt, o.createdAt) as date)
            order by cast(coalesce(o.leaseStartAt, o.createdAt) as date)
            """)
    List<DailyMetric> aggregateDailyMetrics(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("""
            select cast(coalesce(o.leaseStartAt, o.createdAt) as date) as day,
                   count(o) as orderCount,
                   coalesce(sum(o.totalAmount), 0) as totalAmount
            from RentalOrder o
            where o.vendorId = :vendorId and coalesce(o.leaseStartAt, o.createdAt) between :start and :end
            group by cast(coalesce(o.leaseStartAt, o.createdAt) as date)
            order by cast(coalesce(o.leaseStartAt, o.createdAt) as date)
            """)
    List<DailyMetric> aggregateDailyMetricsByVendor(@Param("vendorId") UUID vendorId,
                                                    @Param("start") OffsetDateTime start,
                                                    @Param("end") OffsetDateTime end);

    @Query("""
            select coalesce(o.planType, 'UNKNOWN') as planType,
                   count(o) as orderCount,
                   coalesce(sum(o.totalAmount), 0) as totalAmount
            from RentalOrder o
            group by coalesce(o.planType, 'UNKNOWN')
            """)
    List<PlanTypeAggregate> aggregatePlanTypeMetrics();

    @Query("""
            select coalesce(o.planType, 'UNKNOWN') as planType,
                   count(o) as orderCount,
                   coalesce(sum(o.totalAmount), 0) as totalAmount
            from RentalOrder o
            where o.vendorId = :vendorId
            group by coalesce(o.planType, 'UNKNOWN')
            """)
    List<PlanTypeAggregate> aggregatePlanTypeMetricsByVendor(@Param("vendorId") UUID vendorId);

    interface OrderStatusCount {
        OrderStatus getStatus();

        long getCount();
    }

    interface DailyMetric {
        LocalDate getDay();

        long getOrderCount();

        BigDecimal getTotalAmount();
    }

    interface PlanTypeAggregate {
        String getPlanType();

        long getOrderCount();

        BigDecimal getTotalAmount();
    }
}
