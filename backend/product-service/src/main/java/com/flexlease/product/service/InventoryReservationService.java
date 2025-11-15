package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.config.InventoryConcurrencyProperties;
import com.flexlease.product.domain.InventoryChangeType;
import com.flexlease.product.domain.InventorySnapshot;
import com.flexlease.product.domain.ProductSku;
import com.flexlease.product.dto.InventoryReservationBatchRequest;
import com.flexlease.product.dto.InventoryReservationItemRequest;
import com.flexlease.product.repository.InventorySnapshotRepository;
import com.flexlease.product.repository.ProductSkuRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InventoryReservationService {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReservationService.class);

    private final ProductSkuRepository productSkuRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final TransactionTemplate transactionTemplate;
    private final int maxAttempts;
    private final Duration backoff;

    public InventoryReservationService(ProductSkuRepository productSkuRepository,
                                       InventorySnapshotRepository inventorySnapshotRepository,
                                       PlatformTransactionManager transactionManager,
                                       InventoryConcurrencyProperties concurrencyProperties) {
        this.productSkuRepository = productSkuRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        InventoryConcurrencyProperties.Concurrency concurrency = concurrencyProperties.getConcurrency();
        this.maxAttempts = Math.max(1, concurrency.getMaxAttempts());
        Duration configuredBackoff = concurrency.getBackoff();
        this.backoff = configuredBackoff == null ? Duration.ZERO : configuredBackoff;
    }

    public void processReservations(InventoryReservationBatchRequest request) {
        int attempt = 0;
        while (true) {
            try {
                transactionTemplate.executeWithoutResult(status -> processOnce(request));
                return;
            } catch (OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= maxAttempts) {
                    LOG.error("Inventory reservation failed after {} attempts for reference {}", attempt, request.referenceId());
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "库存繁忙，请稍后重试");
                }
                LOG.debug("Retrying inventory reservation for reference {} due to concurrent update (attempt {})",
                        request.referenceId(), attempt + 1);
                applyBackoff(attempt);
            }
        }
    }

    private void processOnce(InventoryReservationBatchRequest request) {
        UUID referenceId = request.referenceId();
        List<InventoryReservationItemRequest> items = request.items();
        for (InventoryReservationItemRequest item : items) {
            ProductSku sku = productSkuRepository.findById(item.skuId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SKU 不存在"));
            int quantity = item.quantity();
            int signedQuantity;
            try {
                signedQuantity = applyChange(sku, item.changeType(), quantity);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            }
            inventorySnapshotRepository.save(InventorySnapshot.record(
                    sku,
                    item.changeType(),
                    signedQuantity,
                    sku.getStockAvailable(),
                    referenceId
            ));
        }
        LOG.debug("Processed {} inventory commands for reference {}", items.size(), referenceId);
    }

    private void applyBackoff(int attempt) {
        if (backoff == null || backoff.isZero() || backoff.isNegative()) {
            return;
        }
        long nanos = backoff.toNanos() * (long) attempt;
        if (nanos <= 0L) {
            return;
        }
        LockSupport.parkNanos(nanos);
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "库存处理中断，请重试");
        }
    }

    private int applyChange(ProductSku sku, InventoryChangeType changeType, int quantity) {
        return switch (changeType) {
            case RESERVE -> {
                sku.reserve(quantity);
                yield -quantity;
            }
            case RELEASE -> {
                sku.release(quantity);
                yield quantity;
            }
            case INBOUND -> {
                sku.inbound(quantity);
                yield quantity;
            }
            case OUTBOUND -> {
                sku.outbound(quantity);
                yield -quantity;
            }
        };
    }
}
