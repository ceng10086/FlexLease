package com.flexlease.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.product.domain.MediaAsset;
import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductSku;
import com.flexlease.product.domain.ProductSkuStatus;
import com.flexlease.product.domain.RentalPlan;
import com.flexlease.product.domain.RentalPlanStatus;
import com.flexlease.product.dto.CatalogProductResponse;
import com.flexlease.product.dto.MediaAssetResponse;
import com.flexlease.product.dto.ProductResponse;
import com.flexlease.product.dto.ProductSummaryResponse;
import com.flexlease.product.dto.RentalPlanResponse;
import com.flexlease.product.dto.SkuResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductAssembler {

    private final ObjectMapper objectMapper;

    public ProductAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProductResponse toProductResponse(Product product) {
        List<RentalPlanResponse> plans = product.getRentalPlans().stream()
                .map(this::toRentalPlanResponse)
                .toList();
        List<MediaAssetResponse> mediaAssets = product.getMediaAssets().stream()
                .map(this::toMediaAssetResponse)
                .toList();
        return new ProductResponse(
                product.getId(),
                product.getVendorId(),
                product.getName(),
                product.getCategoryCode(),
                product.getDescription(),
                product.getCoverImageUrl(),
                product.getStatus(),
                product.getReviewRemark(),
                product.getSubmittedAt(),
                product.getReviewedBy(),
                product.getReviewedAt(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                mediaAssets,
                plans
        );
    }

    public ProductSummaryResponse toSummary(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getVendorId(),
                product.getName(),
                product.getCategoryCode(),
                product.getStatus(),
                product.getSubmittedAt(),
                product.getReviewedAt(),
                product.getCreatedAt()
        );
    }

    public RentalPlanResponse toRentalPlanResponse(RentalPlan plan) {
        List<SkuResponse> skus = plan.getSkus().stream().map(this::toSkuResponse).toList();
        return new RentalPlanResponse(
                plan.getId(),
                plan.getPlanType(),
                plan.getTermMonths(),
                plan.getDepositAmount(),
                plan.getRentAmountMonthly(),
                plan.getBuyoutPrice(),
                plan.isAllowExtend(),
                plan.getExtensionUnit(),
                plan.getExtensionPrice(),
                plan.getStatus(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                skus
        );
    }

    public SkuResponse toSkuResponse(ProductSku sku) {
        return new SkuResponse(
                sku.getId(),
                sku.getSkuCode(),
                readAttributes(sku.getAttributes()),
                sku.getStockTotal(),
                sku.getStockAvailable(),
                sku.getStatus(),
                sku.getCreatedAt(),
                sku.getUpdatedAt()
        );
    }

    public String writeAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("SKU 属性格式错误", e);
        }
    }

    private Map<String, Object> readAttributes(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    public CatalogProductResponse toCatalog(Product product) {
        List<CatalogProductResponse.MediaAssetItem> mediaItems = product.getMediaAssets().stream()
                .map(asset -> new CatalogProductResponse.MediaAssetItem(
                        asset.getId(),
                        asset.getFileUrl(),
                        asset.getSortOrder()
                ))
                .toList();
        List<CatalogProductResponse.RentalPlanItem> planItems = product.getRentalPlans().stream()
                .filter(plan -> plan.getStatus() == RentalPlanStatus.ACTIVE)
                .map(plan -> new CatalogProductResponse.RentalPlanItem(
                        plan.getId(),
                        plan.getPlanType(),
                        plan.getTermMonths(),
                        plan.getDepositAmount(),
                        plan.getRentAmountMonthly(),
                        plan.getBuyoutPrice(),
                        plan.isAllowExtend(),
                        plan.getExtensionUnit(),
                        plan.getExtensionPrice(),
                        plan.getSkus().stream()
                                .filter(sku -> sku.getStatus() == ProductSkuStatus.ACTIVE)
                                .map(sku -> new CatalogProductResponse.CatalogSkuItem(
                                        sku.getId(),
                                        sku.getSkuCode(),
                                        readAttributes(sku.getAttributes()),
                                        sku.getStockTotal(),
                                        sku.getStockAvailable()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
        return new CatalogProductResponse(
                product.getId(),
                product.getVendorId(),
                product.getName(),
                product.getCategoryCode(),
                product.getDescription(),
                product.getCoverImageUrl(),
                product.getStatus(),
                mediaItems,
                planItems
        );
    }

    public MediaAssetResponse toMediaAssetResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getFileName(),
                asset.getFileUrl(),
                asset.getContentType(),
                asset.getFileSize(),
                asset.getSortOrder(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
