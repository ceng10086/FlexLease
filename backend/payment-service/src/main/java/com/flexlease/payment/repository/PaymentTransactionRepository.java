package com.flexlease.payment.repository;

import com.flexlease.payment.domain.PaymentScene;
import com.flexlease.payment.domain.PaymentStatus;
import com.flexlease.payment.domain.PaymentTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 支付流水仓储。
 *
 * <p>提供按订单+场景查询待支付流水的能力，配合幂等与重复点击防护。</p>
 */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>,
        JpaSpecificationExecutor<PaymentTransaction> {

    Optional<PaymentTransaction> findByTransactionNo(String transactionNo);

    Optional<PaymentTransaction> findFirstByOrderIdAndSceneAndStatus(UUID orderId,
                                                                     PaymentScene scene,
                                                                     PaymentStatus status);
}
