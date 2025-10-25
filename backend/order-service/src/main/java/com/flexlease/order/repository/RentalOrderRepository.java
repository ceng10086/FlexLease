package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderStatus;
import com.flexlease.order.domain.RentalOrder;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalOrderRepository extends JpaRepository<RentalOrder, UUID> {

    Optional<RentalOrder> findWithDetailsById(UUID id);

    Page<RentalOrder> findByUserId(UUID userId, Pageable pageable);

    Page<RentalOrder> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Page<RentalOrder> findByVendorId(UUID vendorId, Pageable pageable);

    Page<RentalOrder> findByVendorIdAndStatus(UUID vendorId, OrderStatus status, Pageable pageable);

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

    interface OrderStatusCount {
        OrderStatus getStatus();

        long getCount();
    }
}
