package com.flexlease.payment.repository;

import com.flexlease.payment.domain.PaymentScene;
import com.flexlease.payment.domain.PaymentStatus;
import com.flexlease.payment.domain.PaymentTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>,
        JpaSpecificationExecutor<PaymentTransaction> {

    Optional<PaymentTransaction> findByTransactionNo(String transactionNo);

    Optional<PaymentTransaction> findFirstByOrderIdAndSceneAndStatus(UUID orderId,
                                                                     PaymentScene scene,
                                                                     PaymentStatus status);
}
