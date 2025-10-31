package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.domain.InventoryChangeType;
import com.flexlease.product.domain.InventorySnapshot;
import com.flexlease.product.domain.ProductSku;
import com.flexlease.product.dto.InventoryReservationBatchRequest;
import com.flexlease.product.dto.InventoryReservationItemRequest;
import com.flexlease.product.repository.InventorySnapshotRepository;
import com.flexlease.product.repository.ProductSkuRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class InventoryReservationService {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReservationService.class);

    private final ProductSkuRepository productSkuRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;

    public InventoryReservationService(ProductSkuRepository productSkuRepository,
                                       InventorySnapshotRepository inventorySnapshotRepository) {
        this.productSkuRepository = productSkuRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
    }

    public void processReservations(InventoryReservationBatchRequest request) {
        UUID referenceId = request.referenceId();
        List<InventoryReservationItemRequest> items = request.items();
        for (InventoryReservationItemRequest item : items) {
            ProductSku sku = productSkuRepository.findByIdForUpdate(item.skuId())
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
