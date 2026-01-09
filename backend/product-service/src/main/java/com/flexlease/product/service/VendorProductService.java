package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.domain.InventoryChangeType;
import com.flexlease.product.domain.InventorySnapshot;
import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductSku;
import com.flexlease.product.domain.ProductSkuStatus;
import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.domain.RentalPlan;
import com.flexlease.product.domain.RentalPlanStatus;
import com.flexlease.product.dto.InventoryAdjustRequest;
import com.flexlease.product.dto.PagedResponse;
import com.flexlease.product.dto.ProductRequest;
import com.flexlease.product.dto.ProductResponse;
import com.flexlease.product.dto.ProductShelveRequest;
import com.flexlease.product.dto.ProductSummaryResponse;
import com.flexlease.product.dto.RentalPlanRequest;
import com.flexlease.product.dto.RentalPlanResponse;
import com.flexlease.product.dto.SkuRequest;
import com.flexlease.product.dto.SkuResponse;
import com.flexlease.product.repository.InventorySnapshotRepository;
import com.flexlease.product.repository.ProductRepository;
import com.flexlease.product.repository.ProductSkuRepository;
import com.flexlease.product.repository.RentalPlanRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class VendorProductService {

    /**
     * 厂商侧商品管理服务。
     * <p>
     * 聚合商品、租赁方案、SKU、库存流水等操作；并负责对“是否属于当前厂商”的权限边界做二次校验。
     */
    private final ProductRepository productRepository;
    private final RentalPlanRepository rentalPlanRepository;
    private final ProductSkuRepository productSkuRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ProductAssembler assembler;

    public VendorProductService(ProductRepository productRepository,
                                RentalPlanRepository rentalPlanRepository,
                                ProductSkuRepository productSkuRepository,
                                InventorySnapshotRepository inventorySnapshotRepository,
                                ProductAssembler assembler) {
        this.productRepository = productRepository;
        this.rentalPlanRepository = rentalPlanRepository;
        this.productSkuRepository = productSkuRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.assembler = assembler;
    }

    public ProductResponse createProduct(UUID vendorId, ProductRequest request) {
        Product product = Product.create(vendorId,
                request.name(),
                request.categoryCode(),
                request.description(),
                request.coverImageUrl());
        productRepository.save(product);
        return assembler.toProductResponse(product);
    }

    public ProductResponse updateProduct(UUID vendorId, UUID productId, ProductRequest request) {
        Product product = getOwnProduct(vendorId, productId);
        if (product.getStatus() == ProductStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "商品审核中，暂不可修改");
        }
        product.updateBasicInfo(request.name(), request.categoryCode(), request.description(), request.coverImageUrl());
        return assembler.toProductResponse(product);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResponse<ProductSummaryResponse> listProducts(UUID vendorId,
                                                               ProductStatus status,
                                                               String keyword,
                                                               Pageable pageable) {
        Page<Product> page;
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (status != null && hasKeyword) {
            page = productRepository.findByVendorIdAndStatusAndNameContainingIgnoreCase(vendorId, status, keyword, pageable);
        } else if (status != null) {
            page = productRepository.findByVendorIdAndStatus(vendorId, status, pageable);
        } else if (hasKeyword) {
            page = productRepository.findByVendorIdAndNameContainingIgnoreCase(vendorId, keyword, pageable);
        } else {
            page = productRepository.findByVendorId(vendorId, pageable);
        }
        return toPagedResponse(page, assembler::toSummary);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProductResponse getProduct(UUID vendorId, UUID productId) {
        Product product = productRepository.findWithPlansByIdAndVendorId(productId, vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));
        return assembler.toProductResponse(product);
    }

    public ProductResponse submitForReview(UUID vendorId, UUID productId) {
        Product product = getOwnProduct(vendorId, productId);
        if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅草稿或已驳回的商品可提交审核");
        }
        if (product.getRentalPlans().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请至少配置一个租赁方案后再提交");
        }
        product.resetReviewRemark();
        product.submitForReview();
        return assembler.toProductResponse(product);
    }

    public ProductResponse changeShelveStatus(UUID vendorId, UUID productId, ProductShelveRequest request) {
        Product product = getOwnProduct(vendorId, productId);
        if (request.publish()) {
            if (product.getStatus() != ProductStatus.INACTIVE) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅下架状态的商品可再次上架");
            }
            product.activate();
        } else {
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅上架状态的商品可下架");
            }
            product.markInactive();
        }
        return assembler.toProductResponse(product);
    }

    public List<RentalPlanResponse> listPlans(UUID vendorId, UUID productId) {
        Product product = productRepository.findWithPlansByIdAndVendorId(productId, vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));
        return product.getRentalPlans().stream().map(assembler::toRentalPlanResponse).toList();
    }

    public RentalPlanResponse createPlan(UUID vendorId, UUID productId, RentalPlanRequest request) {
        Product product = getOwnProduct(vendorId, productId);
        RentalPlan plan = RentalPlan.create(product,
            request.planType(),
            request.termMonths(),
            ensurePositive(request.depositAmount(), "押金需大于等于 0"),
            ensurePositive(request.rentAmountMonthly(), "月租金需大于等于 0"),
            normalizeNullable(request.buyoutPrice(), "买断价需大于等于 0"),
            request.allowExtend(),
            request.extensionUnit(),
            normalizeNullable(request.extensionPrice(), "续租费用需大于等于 0"));
        rentalPlanRepository.save(plan);
        return assembler.toRentalPlanResponse(plan);
    }

    public RentalPlanResponse updatePlan(UUID vendorId, UUID productId, UUID planId, RentalPlanRequest request) {
        RentalPlan plan = getPlanForVendor(vendorId, productId, planId);
        if (plan.getStatus() == RentalPlanStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "启用中的方案请先停用再修改");
        }
        plan.updateBasicInfo(request.planType(),
                request.termMonths(),
                ensurePositive(request.depositAmount(), "押金需大于等于 0"),
                ensurePositive(request.rentAmountMonthly(), "月租金需大于等于 0"),
            normalizeNullable(request.buyoutPrice(), "买断价需大于等于 0"),
                request.allowExtend(),
                request.extensionUnit(),
            normalizeNullable(request.extensionPrice(), "续租费用需大于等于 0"));
        return assembler.toRentalPlanResponse(plan);
    }

    public RentalPlanResponse activatePlan(UUID vendorId, UUID productId, UUID planId) {
        RentalPlan plan = getPlanForVendor(vendorId, productId, planId);
        if (plan.getStatus() == RentalPlanStatus.ACTIVE) {
            return assembler.toRentalPlanResponse(plan);
        }
        plan.activate();
        return assembler.toRentalPlanResponse(plan);
    }

    public RentalPlanResponse deactivatePlan(UUID vendorId, UUID productId, UUID planId) {
        RentalPlan plan = getPlanForVendor(vendorId, productId, planId);
        if (plan.getStatus() == RentalPlanStatus.INACTIVE) {
            return assembler.toRentalPlanResponse(plan);
        }
        plan.deactivate();
        return assembler.toRentalPlanResponse(plan);
    }

    public SkuResponse createSku(UUID vendorId, UUID productId, UUID planId, SkuRequest request) {
        RentalPlan plan = getPlanForVendor(vendorId, productId, planId);
        productSkuRepository.findBySkuCodeIgnoreCase(request.skuCode()).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "SKU 编码已存在");
        });
        String attributesJson = assembler.writeAttributes(request.attributes());
        ProductSku sku = ProductSku.create(plan.getProduct(), plan, request.skuCode(), attributesJson, request.stockTotal());
        if (request.status() != null && request.status() != ProductSkuStatus.ACTIVE) {
            sku.setStatus(request.status());
        }
        if (request.stockAvailable() != null) {
            int available = request.stockAvailable();
            if (available > sku.getStockTotal()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "可用库存不能大于库存总量");
            }
            sku.updateBasicInfo(sku.getSkuCode(), attributesJson, sku.getStockTotal(), available, sku.getStatus());
        }
        productSkuRepository.save(sku);
        return assembler.toSkuResponse(sku);
    }

    public SkuResponse updateSku(UUID vendorId, UUID productId, UUID planId, UUID skuId, SkuRequest request) {
        RentalPlan plan = getPlanForVendor(vendorId, productId, planId);
        ProductSku sku = productSkuRepository.findByIdAndProductId(skuId, plan.getProduct().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SKU 不存在"));
        productSkuRepository.findBySkuCodeIgnoreCase(request.skuCode()).ifPresent(existing -> {
            if (!existing.getId().equals(sku.getId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "SKU 编码已存在");
            }
        });
        String attributesJson = assembler.writeAttributes(request.attributes());
        int available = request.stockAvailable() != null ? request.stockAvailable() : sku.getStockAvailable();
        if (available > request.stockTotal()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "可用库存不能大于库存总量");
        }
        ProductSkuStatus status = request.status() != null ? request.status() : sku.getStatus();
        sku.updateBasicInfo(request.skuCode(), attributesJson, request.stockTotal(), available, status);
        if (!plan.equals(sku.getRentalPlan())) {
            sku.setRentalPlan(plan);
        }
        return assembler.toSkuResponse(sku);
    }

    public SkuResponse adjustInventory(UUID vendorId, UUID productId, UUID planId, UUID skuId, InventoryAdjustRequest request) {
        RentalPlan plan = getPlanForVendor(vendorId, productId, planId);
        ProductSku sku = productSkuRepository.findByIdAndProductId(skuId, plan.getProduct().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SKU 不存在"));
        int quantity = request.quantity();
        int signedQty;
        try {
            switch (request.changeType()) {
                case INBOUND -> {
                    sku.inbound(quantity);
                    signedQty = quantity;
                }
                case OUTBOUND -> {
                    sku.outbound(quantity);
                    signedQty = -quantity;
                }
                case RESERVE -> {
                    sku.reserve(quantity);
                    signedQty = -quantity;
                }
                case RELEASE -> {
                    sku.release(quantity);
                    signedQty = quantity;
                }
                default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的库存调整类型");
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
        inventorySnapshotRepository.save(InventorySnapshot.record(sku, request.changeType(), signedQty, sku.getStockAvailable(), request.referenceId()));
        productSkuRepository.save(sku);
        return assembler.toSkuResponse(sku);
    }

    private Product getOwnProduct(UUID vendorId, UUID productId) {
        return productRepository.findByIdAndVendorId(productId, vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));
    }

    private RentalPlan getPlanForVendor(UUID vendorId, UUID productId, UUID planId) {
        RentalPlan plan = rentalPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "租赁方案不存在"));
        if (!plan.getProduct().getVendorId().equals(vendorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此租赁方案");
        }
        if (!plan.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "租赁方案不属于指定商品");
        }
        return plan;
    }

    private BigDecimal ensurePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }

    private BigDecimal normalizeNullable(BigDecimal value, String message) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }

    private <T, R> PagedResponse<R> toPagedResponse(Page<T> page, Function<T, R> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
