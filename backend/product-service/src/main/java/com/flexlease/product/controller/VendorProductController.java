package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.product.domain.ProductStatus;
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
import com.flexlease.product.service.VendorProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/products")
public class VendorProductController {

    private final VendorProductService vendorProductService;

    public VendorProductController(VendorProductService vendorProductService) {
        this.vendorProductService = vendorProductService;
    }

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@PathVariable UUID vendorId,
                                                       @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(vendorProductService.createProduct(vendorId, request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable UUID vendorId,
                                                       @PathVariable UUID productId,
                                                       @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(vendorProductService.updateProduct(vendorId, productId, request));
    }

    @PostMapping("/{productId}/submit")
    public ApiResponse<ProductResponse> submitProduct(@PathVariable UUID vendorId,
                                                       @PathVariable UUID productId) {
        return ApiResponse.success(vendorProductService.submitForReview(vendorId, productId));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable UUID vendorId,
                                                    @PathVariable UUID productId) {
        return ApiResponse.success(vendorProductService.getProduct(vendorId, productId));
    }

    @GetMapping
    public ApiResponse<PagedResponse<ProductSummaryResponse>> listProducts(@PathVariable UUID vendorId,
                                                                            @RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(required = false) ProductStatus status,
                                                                            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(vendorProductService.listProducts(vendorId, status, keyword, pageable));
    }

    @PostMapping("/{productId}/shelve")
    public ApiResponse<ProductResponse> shelve(@PathVariable UUID vendorId,
                                                @PathVariable UUID productId,
                                                @Valid @RequestBody ProductShelveRequest request) {
        return ApiResponse.success(vendorProductService.changeShelveStatus(vendorId, productId, request));
    }

    @GetMapping("/{productId}/rental-plans")
    public ApiResponse<List<RentalPlanResponse>> listPlans(@PathVariable UUID vendorId,
                                                           @PathVariable UUID productId) {
        return ApiResponse.success(vendorProductService.listPlans(vendorId, productId));
    }

    @PostMapping("/{productId}/rental-plans")
    public ApiResponse<RentalPlanResponse> createRentalPlan(@PathVariable UUID vendorId,
                                                             @PathVariable UUID productId,
                                                             @Valid @RequestBody RentalPlanRequest request) {
        return ApiResponse.success(vendorProductService.createPlan(vendorId, productId, request));
    }

    @PutMapping("/{productId}/rental-plans/{planId}")
    public ApiResponse<RentalPlanResponse> updateRentalPlan(@PathVariable UUID vendorId,
                                                             @PathVariable UUID productId,
                                                             @PathVariable UUID planId,
                                                             @Valid @RequestBody RentalPlanRequest request) {
        return ApiResponse.success(vendorProductService.updatePlan(vendorId, productId, planId, request));
    }

    @PostMapping("/{productId}/rental-plans/{planId}/activate")
    public ApiResponse<RentalPlanResponse> activateRentalPlan(@PathVariable UUID vendorId,
                                                               @PathVariable UUID productId,
                                                               @PathVariable UUID planId) {
        return ApiResponse.success(vendorProductService.activatePlan(vendorId, productId, planId));
    }

    @PostMapping("/{productId}/rental-plans/{planId}/deactivate")
    public ApiResponse<RentalPlanResponse> deactivateRentalPlan(@PathVariable UUID vendorId,
                                                                 @PathVariable UUID productId,
                                                                 @PathVariable UUID planId) {
        return ApiResponse.success(vendorProductService.deactivatePlan(vendorId, productId, planId));
    }

    @PostMapping("/{productId}/rental-plans/{planId}/skus")
    public ApiResponse<SkuResponse> createSku(@PathVariable UUID vendorId,
                                               @PathVariable UUID productId,
                                               @PathVariable UUID planId,
                                               @Valid @RequestBody SkuRequest request) {
        return ApiResponse.success(vendorProductService.createSku(vendorId, productId, planId, request));
    }

    @PutMapping("/{productId}/rental-plans/{planId}/skus/{skuId}")
    public ApiResponse<SkuResponse> updateSku(@PathVariable UUID vendorId,
                                               @PathVariable UUID productId,
                                               @PathVariable UUID planId,
                                               @PathVariable UUID skuId,
                                               @Valid @RequestBody SkuRequest request) {
        return ApiResponse.success(vendorProductService.updateSku(vendorId, productId, planId, skuId, request));
    }

    @PostMapping("/{productId}/rental-plans/{planId}/skus/{skuId}/inventory/adjust")
    public ApiResponse<SkuResponse> adjustInventory(@PathVariable UUID vendorId,
                                                     @PathVariable UUID productId,
                                                     @PathVariable UUID planId,
                                                     @PathVariable UUID skuId,
                                                     @Valid @RequestBody InventoryAdjustRequest request) {
        return ApiResponse.success(vendorProductService.adjustInventory(vendorId, productId, planId, skuId, request));
    }
}
