package com.flexlease.payment.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.payment.domain.PaymentScene;
import com.flexlease.payment.domain.PaymentSplit;
import com.flexlease.payment.domain.PaymentSplitType;
import com.flexlease.payment.domain.PaymentStatus;
import com.flexlease.payment.domain.PaymentTransaction;
import com.flexlease.payment.domain.RefundStatus;
import com.flexlease.payment.domain.RefundTransaction;
import com.flexlease.payment.client.NotificationClient;
import com.flexlease.payment.client.OrderServiceClient;
import com.flexlease.payment.client.VendorServiceClient;
import com.flexlease.payment.dto.PaymentCallbackRequest;
import com.flexlease.payment.dto.PaymentConfirmRequest;
import com.flexlease.payment.dto.PaymentInitRequest;
import com.flexlease.payment.dto.PaymentRefundRequest;
import com.flexlease.payment.dto.PaymentSettlementResponse;
import com.flexlease.payment.dto.PaymentSplitRequest;
import com.flexlease.payment.dto.PaymentTransactionResponse;
import com.flexlease.payment.dto.RefundTransactionResponse;
import com.flexlease.payment.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PaymentTransactionService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentTransactionService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentAssembler assembler;
    private final NotificationClient notificationClient;
    private final OrderServiceClient orderServiceClient;
    private final VendorServiceClient vendorServiceClient;
    private final boolean autoConfirmPayments;

    public PaymentTransactionService(PaymentTransactionRepository paymentTransactionRepository,
                                     PaymentAssembler assembler,
                                     NotificationClient notificationClient,
                                     OrderServiceClient orderServiceClient,
                                     VendorServiceClient vendorServiceClient,
                                     @Value("${flexlease.payment.auto-confirm:true}") boolean autoConfirmPayments) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.assembler = assembler;
        this.notificationClient = notificationClient;
        this.orderServiceClient = orderServiceClient;
        this.vendorServiceClient = vendorServiceClient;
        this.autoConfirmPayments = autoConfirmPayments;
    }

    public PaymentTransactionResponse initPayment(UUID orderId, PaymentInitRequest request) {
        SecurityUtils.getCurrentPrincipal().ifPresent(principal -> validateInitPermission(principal, request));
        paymentTransactionRepository.findFirstByOrderIdAndSceneAndStatus(orderId, request.scene(), PaymentStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "存在待支付的同类流水");
                });

        SplitBuildResult splitResult = buildSplits(request.vendorId(), request.splits());
        if (splitResult.totalAmount().compareTo(request.amount()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "分账金额超过支付总额");
        }

        try {
            PaymentTransaction transaction = PaymentTransaction.create(
                    orderId,
                    request.userId(),
                    request.vendorId(),
                    request.scene(),
                    request.channel(),
                    request.amount(),
                    request.description()
            );
            if (splitResult.commissionRate() != null) {
                transaction.setCommissionRate(splitResult.commissionRate());
            }
            splitResult.splits().forEach(transaction::addSplit);
            PaymentTransaction saved = paymentTransactionRepository.save(transaction);
            autoConfirmIfEnabled(saved);
            return assembler.toResponse(saved);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    public PaymentTransactionResponse getTransaction(UUID transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "支付流水不存在"));
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        validateViewPermission(principal, transaction);
        return assembler.toResponse(transaction);
    }

    public PaymentTransactionResponse confirmPayment(UUID transactionId, PaymentConfirmRequest request) {
        SecurityUtils.getCurrentPrincipal().ifPresent(principal -> {
            if (!principal.hasRole("ADMIN") && !principal.hasRole("INTERNAL")) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份无权确认支付");
            }
        });
        PaymentTransaction transaction = getTransactionForUpdate(transactionId);
        boolean transitioned = false;
        if (transaction.getStatus() == PaymentStatus.PENDING) {
            try {
                transaction.markSucceeded(request.channelTransactionNo(), request.paidAt());
                transitioned = true;
            } catch (IllegalStateException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            }
        } else if (transaction.getStatus() == PaymentStatus.SUCCEEDED) {
            LOG.debug("Payment transaction {} already succeeded, skipping manual transition", transactionId);
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "当前支付状态不支持确认");
        }
        if (transitioned) {
            publishPaymentSuccessEvent(transaction);
        }
        return assembler.toResponse(transaction);
    }

    public PaymentTransactionResponse handleCallback(UUID transactionId, PaymentCallbackRequest request) {
        if (request.status() == PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "回调状态不合法");
        }
        SecurityUtils.getCurrentPrincipal().ifPresent(principal -> {
            if (!principal.hasRole("INTERNAL") && !principal.hasRole("ADMIN")) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份无权执行回调");
            }
        });
        PaymentTransaction transaction = getTransactionForUpdate(transactionId);
        if (request.status() == PaymentStatus.SUCCEEDED) {
            boolean transitioned = false;
            if (transaction.getStatus() == PaymentStatus.PENDING) {
                OffsetDateTime paidAt = request.paidAt() != null ? request.paidAt() : OffsetDateTime.now();
                try {
                    transaction.markSucceeded(request.channelTransactionNo(), paidAt);
                    transitioned = true;
                } catch (IllegalStateException ex) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
                }
            } else if (transaction.getStatus() == PaymentStatus.SUCCEEDED) {
                LOG.debug("Duplicate success callback for transaction {}", transactionId);
            } else if (transaction.getStatus() == PaymentStatus.FAILED) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付流水已标记失败，无法回滚为成功");
            }
            if (transitioned) {
                publishPaymentSuccessEvent(transaction);
            }
        } else {
            if (transaction.getStatus() == PaymentStatus.SUCCEEDED) {
                LOG.warn("Ignoring failure callback for succeeded transaction {}", transactionId);
                return assembler.toResponse(transaction);
            }
            if (transaction.getStatus() == PaymentStatus.FAILED) {
                LOG.debug("Duplicate failure callback for transaction {}", transactionId);
                return assembler.toResponse(transaction);
            }
            try {
                transaction.markFailed(request.channelTransactionNo());
            } catch (IllegalStateException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            }
            notifyPaymentFailed(transaction);
        }
        return assembler.toResponse(transaction);
    }

    public RefundTransactionResponse createRefund(UUID transactionId, PaymentRefundRequest request) {
        SecurityUtils.getCurrentPrincipal().ifPresent(principal -> {
            if (!principal.hasRole("ADMIN") && !principal.hasRole("INTERNAL")) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份无权发起退款");
            }
        });
        PaymentTransaction transaction = getTransactionForUpdate(transactionId);
        try {
            RefundTransaction refund = transaction.createRefund(request.amount(), request.reason());
            // 模拟通道立即退款成功
            refund.markSucceeded();
                notifyRefundSucceeded(transaction, refund);
            return assembler.toResponse(transaction).refunds().stream()
                    .filter(r -> r.id().equals(refund.getId()))
                    .findFirst()
                    .orElseThrow();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    private void autoConfirmIfEnabled(PaymentTransaction transaction) {
        if (!autoConfirmPayments || transaction.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        boolean transitioned = false;
        try {
            transaction.markSucceeded("AUTO-CONFIRMED", OffsetDateTime.now());
            transitioned = true;
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
        if (transitioned) {
            LOG.debug("Payment {} auto-confirmed, scheduling order notification", transaction.getId());
            publishPaymentSuccessEvent(transaction);
        }
    }

    public List<PaymentSettlementResponse> calculateSettlements(UUID vendorId,
                                                                 OffsetDateTime from,
                                                                 OffsetDateTime to,
                                                                 OffsetDateTime refundFrom,
                                                                 OffsetDateTime refundTo) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "开始时间不能晚于结束时间");
        }
        if (refundFrom != null && refundTo != null && refundFrom.isAfter(refundTo)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "退款开始时间不能晚于结束时间");
        }
        Specification<PaymentTransaction> specification = (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates.getExpressions().add(cb.equal(root.get("status"), PaymentStatus.SUCCEEDED));
            if (vendorId != null) {
                predicates.getExpressions().add(cb.equal(root.get("vendorId"), vendorId));
            }
            if (from != null) {
                predicates.getExpressions().add(cb.greaterThanOrEqualTo(root.get("paidAt"), from));
            }
            if (to != null) {
                predicates.getExpressions().add(cb.lessThanOrEqualTo(root.get("paidAt"), to));
            }
            return predicates;
        };
        List<PaymentTransaction> transactions = paymentTransactionRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "paidAt"));
        Map<UUID, SettlementAccumulator> grouped = new LinkedHashMap<>();
        for (PaymentTransaction transaction : transactions) {
            SettlementAccumulator accumulator = grouped.computeIfAbsent(transaction.getVendorId(), key -> new SettlementAccumulator(refundFrom, refundTo));
            accumulator.add(transaction);
        }
        return grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
            .map(entry -> entry.getValue().toResponse(entry.getKey()))
            .toList();
    }

    private PaymentTransaction getTransactionForUpdate(UUID transactionId) {
        return paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "支付流水不存在"));
    }

    private SplitBuildResult buildSplits(UUID vendorId, List<PaymentSplitRequest> rawSplits) {
        if (rawSplits == null || rawSplits.isEmpty()) {
            return new SplitBuildResult(List.of(), BigDecimal.ZERO, null);
        }
        List<PaymentSplit> sanitized = new ArrayList<>();
        BigDecimal vendorIncome = BigDecimal.ZERO;
        String vendorBeneficiary = null;
        for (PaymentSplitRequest splitRequest : rawSplits) {
            if (splitRequest == null || splitRequest.splitType() == null) {
                continue;
            }
            BigDecimal amount = splitRequest.amount();
            if (amount == null || amount.signum() <= 0) {
                continue;
            }
            PaymentSplitType type = splitRequest.splitType();
            if (type == PaymentSplitType.VENDOR_INCOME) {
                vendorIncome = vendorIncome.add(amount);
                if (splitRequest.beneficiary() != null && !splitRequest.beneficiary().isBlank()) {
                    vendorBeneficiary = splitRequest.beneficiary();
                }
                continue;
            }
            if (type == PaymentSplitType.PLATFORM_COMMISSION) {
                continue;
            }
            sanitized.add(PaymentSplit.create(type, amount, splitRequest.beneficiary()));
        }
        BigDecimal commissionRate = null;
        if (vendorIncome.compareTo(BigDecimal.ZERO) > 0) {
            commissionRate = resolveCommissionRate(vendorId);
            if (commissionRate == null) {
                commissionRate = BigDecimal.ZERO;
            }
            BigDecimal commissionAmount = BigDecimal.ZERO;
            if (commissionRate.compareTo(BigDecimal.ZERO) > 0) {
                commissionAmount = vendorIncome.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
                if (commissionAmount.compareTo(vendorIncome) > 0) {
                    commissionAmount = vendorIncome;
                }
            }
            BigDecimal vendorNet = vendorIncome.subtract(commissionAmount);
            if (vendorNet.compareTo(BigDecimal.ZERO) > 0) {
                sanitized.add(PaymentSplit.create(PaymentSplitType.VENDOR_INCOME,
                        vendorNet,
                        resolveVendorBeneficiary(vendorBeneficiary, vendorId)));
            }
            if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
                sanitized.add(PaymentSplit.create(PaymentSplitType.PLATFORM_COMMISSION,
                        commissionAmount,
                        "PLATFORM_COMMISSION"));
            }
        } else if (vendorBeneficiary != null) {
            sanitized.add(PaymentSplit.create(PaymentSplitType.VENDOR_INCOME,
                    BigDecimal.ZERO,
                    resolveVendorBeneficiary(vendorBeneficiary, vendorId)));
        }
        BigDecimal total = sanitized.stream()
                .map(PaymentSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SplitBuildResult(sanitized, total, commissionRate);
    }

    private BigDecimal resolveCommissionRate(UUID vendorId) {
        if (vendorId == null) {
            return BigDecimal.ZERO;
        }
        try {
            VendorServiceClient.VendorCommissionProfile profile = vendorServiceClient.loadCommissionProfile(vendorId);
            if (profile != null && profile.commissionRate() != null) {
                return profile.commissionRate();
            }
        } catch (BusinessException ex) {
            LOG.warn("加载厂商 {} 抽成配置失败: {}", vendorId, ex.getMessage());
        } catch (RuntimeException ex) {
            LOG.warn("调用厂商服务获取抽成配置失败: {}", ex.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private String resolveVendorBeneficiary(String provided, UUID vendorId) {
        if (provided != null && !provided.isBlank()) {
            return provided;
        }
        return vendorId == null ? "VENDOR_UNKNOWN" : "VENDOR_" + vendorId;
    }

    private void validateInitPermission(FlexleasePrincipal principal, PaymentInitRequest request) {
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        if (!principal.hasRole("USER")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份无权创建支付");
        }
        if (principal.userId() == null || !principal.userId().equals(request.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止使用他人账号创建支付");
        }
    }

    private void validateViewPermission(FlexleasePrincipal principal, PaymentTransaction transaction) {
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        if (principal.hasRole("USER")) {
            UUID principalUserId = principal.userId();
            if (principalUserId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
            }
            if (transaction.getUserId() == null || !transaction.getUserId().equals(principalUserId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该支付流水");
            }
            return;
        }
        if (principal.hasRole("VENDOR")) {
            UUID currentVendorId = principal.vendorId();
            if (currentVendorId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
            }
            if (transaction.getVendorId() == null || !transaction.getVendorId().equals(currentVendorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该支付流水");
            }
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份无权查看支付流水");
    }

    private void notifyPaymentSucceeded(PaymentSuccessContext context) {
        String sceneName = sceneLabel(context.scene());
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                context.userId().toString(),
                "支付成功",
                "订单 %s 的%s支付 ¥%s 已完成。".formatted(context.orderId(), sceneName, context.amount()),
                Map.of("orderId", context.orderId().toString())
        );
        notificationClient.send(request);
    }

    private void notifyPaymentFailed(PaymentTransaction transaction) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                transaction.getUserId().toString(),
                "支付失败",
                "订单 %s 的支付未成功，请检查后重试。".formatted(transaction.getOrderId()),
                Map.of("orderId", transaction.getOrderId().toString())
        );
        notificationClient.send(request);
    }

    private void notifyRefundSucceeded(PaymentTransaction transaction, RefundTransaction refund) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                transaction.getUserId().toString(),
                "退款成功",
                "订单 %s 的退款 ¥%s 已退回。".formatted(transaction.getOrderId(), refund.getAmount()),
                Map.of("orderId", transaction.getOrderId().toString())
        );
        notificationClient.send(request);
    }

    private String sceneLabel(PaymentScene scene) {
        return switch (scene) {
            case DEPOSIT -> "押金";
            case RENT -> "租金";
            case BUYOUT -> "买断款";
            case PENALTY -> "违约金";
        };
    }

    private void publishPaymentSuccessEvent(PaymentTransaction transaction) {
        PaymentSuccessContext context = new PaymentSuccessContext(
                transaction.getId(),
                transaction.getOrderId(),
                transaction.getUserId(),
                transaction.getScene(),
                transaction.getAmount()
        );
        dispatchPaymentSuccess(context);
    }

    private void dispatchPaymentSuccess(PaymentSuccessContext context) {
        LOG.debug("Payment {} succeeded, notifying order service", context.transactionId());
        orderServiceClient.notifyPaymentSucceeded(context.orderId(), context.transactionId());
        notifyPaymentSucceeded(context);
    }

    private record PaymentSuccessContext(UUID transactionId,
                                         UUID orderId,
                                         UUID userId,
                                         PaymentScene scene,
                                         BigDecimal amount) {
    }

    private static class SettlementAccumulator {
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal deposit = BigDecimal.ZERO;
        private BigDecimal rent = BigDecimal.ZERO;
        private BigDecimal buyout = BigDecimal.ZERO;
        private BigDecimal penalty = BigDecimal.ZERO;
        private BigDecimal platformCommission = BigDecimal.ZERO;
        private BigDecimal refunded = BigDecimal.ZERO;
        private BigDecimal vendorRefunded = BigDecimal.ZERO;
        private OffsetDateTime lastPaidAt;
        private long count;
        private final OffsetDateTime refundFrom;
        private final OffsetDateTime refundTo;

        SettlementAccumulator(OffsetDateTime refundFrom, OffsetDateTime refundTo) {
            this.refundFrom = refundFrom;
            this.refundTo = refundTo;
        }

        void add(PaymentTransaction transaction) {
            BigDecimal transactionAmount = transaction.getAmount();
            total = total.add(transactionAmount);

            BigDecimal depositPortion = BigDecimal.ZERO;
            BigDecimal vendorPortion = BigDecimal.ZERO;
            BigDecimal commissionPortion = BigDecimal.ZERO;

            if (transaction.getSplits() != null && !transaction.getSplits().isEmpty()) {
                for (PaymentSplit split : transaction.getSplits()) {
                    BigDecimal amount = split.getAmount();
                    if (amount == null) {
                        continue;
                    }
                    switch (split.getSplitType()) {
                        case DEPOSIT_RESERVE -> depositPortion = depositPortion.add(amount);
                        case VENDOR_INCOME -> vendorPortion = vendorPortion.add(amount);
                        case PLATFORM_COMMISSION -> commissionPortion = commissionPortion.add(amount);
                    }
                }
            }

            BigDecimal allocated = depositPortion.add(vendorPortion).add(commissionPortion);
            if (allocated.compareTo(transactionAmount) < 0) {
                BigDecimal remainder = transactionAmount.subtract(allocated);
                switch (transaction.getScene()) {
                    case DEPOSIT -> depositPortion = depositPortion.add(remainder);
                    case RENT, BUYOUT, PENALTY -> vendorPortion = vendorPortion.add(remainder);
                }
            }

            deposit = deposit.add(depositPortion);
            platformCommission = platformCommission.add(commissionPortion);

            switch (transaction.getScene()) {
                case RENT -> rent = rent.add(vendorPortion);
                case BUYOUT -> buyout = buyout.add(vendorPortion);
                case PENALTY -> penalty = penalty.add(vendorPortion);
                case DEPOSIT -> {
                    // normally vendor portion should be zero for pure押金，但若存在则归入租金统计以便对账
                    rent = rent.add(vendorPortion);
                }
            }
            OffsetDateTime paidAt = transaction.getPaidAt() != null ? transaction.getPaidAt() : transaction.getUpdatedAt();
            if (lastPaidAt == null || paidAt.isAfter(lastPaidAt)) {
                lastPaidAt = paidAt;
            }
            count += 1;
            transaction.getRefunds().stream()
                    .filter(refund -> refund.getStatus() == RefundStatus.SUCCEEDED)
                    .filter(refund -> isWithinRefundWindow(refund.getRefundedAt()))
                    .forEach(refund -> {
                        BigDecimal amount = refund.getAmount();
                        if (amount == null) {
                            return;
                        }
                        refunded = refunded.add(amount);
                        if (transaction.getScene() != PaymentScene.DEPOSIT) {
                            vendorRefunded = vendorRefunded.add(amount);
                        }
                    });
        }

        PaymentSettlementResponse toResponse(UUID vendorId) {
            BigDecimal vendorEarnings = rent.add(buyout).add(penalty);
            BigDecimal netAmount = vendorEarnings.subtract(vendorRefunded);
            return new PaymentSettlementResponse(vendorId, total, deposit, rent, buyout, penalty, platformCommission, refunded, netAmount, lastPaidAt, count);
        }

        private boolean isWithinRefundWindow(OffsetDateTime refundedAt) {
            if (refundedAt == null) {
                return false;
            }
            if (refundFrom != null && refundedAt.isBefore(refundFrom)) {
                return false;
            }
            if (refundTo != null && refundedAt.isAfter(refundTo)) {
                return false;
            }
            return true;
        }
    }

    private record SplitBuildResult(List<PaymentSplit> splits,
                                    BigDecimal totalAmount,
                                    BigDecimal commissionRate) {
    }
}
