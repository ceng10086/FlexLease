package com.flexlease.order.repository;

import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderSatisfactionSurvey;
import com.flexlease.order.domain.OrderSurveyStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 满意度调查仓库。
 */
public interface OrderSatisfactionSurveyRepository extends JpaRepository<OrderSatisfactionSurvey, UUID> {

    List<OrderSatisfactionSurvey> findByOrderId(UUID orderId);

    @Query("""
            select s from OrderSatisfactionSurvey s
            where s.status = :status and s.availableAt <= :cutoff
            order by s.availableAt asc
            """)
    List<OrderSatisfactionSurvey> findReadyForActivation(@Param("status") OrderSurveyStatus status,
                                                         @Param("cutoff") OffsetDateTime cutoff,
                                                         Pageable pageable);

    boolean existsByDisputeIdAndTargetRoleAndTargetRefAndStatusIn(UUID disputeId,
                                                                  OrderActorRole targetRole,
                                                                  UUID targetRef,
                                                                  Collection<OrderSurveyStatus> statuses);

    long countByStatus(OrderSurveyStatus status);

    @Query("""
            select count(s) from OrderSatisfactionSurvey s
            where s.order.vendorId = :vendorId and s.status = :status
            """)
    long countByVendorAndStatus(@Param("vendorId") UUID vendorId, @Param("status") OrderSurveyStatus status);

    @Query("""
            select coalesce(avg(s.rating), 0)
            from OrderSatisfactionSurvey s
            where s.rating is not null
            """)
    Double averageRating();

    @Query("""
            select coalesce(avg(s.rating), 0)
            from OrderSatisfactionSurvey s
            where s.rating is not null and s.order.vendorId = :vendorId
            """)
    Double averageRatingByVendor(@Param("vendorId") UUID vendorId);

    Optional<OrderSatisfactionSurvey> findByIdAndOrderId(UUID id, UUID orderId);
}
