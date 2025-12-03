package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.dto.FileUploadResponse;
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
import com.flexlease.product.service.ProductAssetService;
import com.flexlease.product.service.VendorProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/products")
public class VendorProductController {

    private final VendorProductService vendorProductService;
    private final ProductAssetService productAssetService;

    public VendorProductController(VendorProductService vendorProductService,
                                   ProductAssetService productAssetService) {
        this.vendorProductService = vendorProductService;
        this.productAssetService = productAssetService;
    }

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@PathVariable UUID vendorId,
                                                       @Valid @RequestBody ProductRequest request) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.createProduct(effectiveVendorId, request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable UUID vendorId,
                                                       @PathVariable UUID productId,
                                                       @Valid @RequestBody ProductRequest request) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.updateProduct(effectiveVendorId, productId, request));
    }

    @PostMapping("/{productId}/submit")
    public ApiResponse<ProductResponse> submitProduct(@PathVariable UUID vendorId,
                                                       @PathVariable UUID productId) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.submitForReview(effectiveVendorId, productId));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable UUID vendorId,
                                                    @PathVariable UUID productId) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.getProduct(effectiveVendorId, productId));
    }

    @GetMapping
    public ApiResponse<PagedResponse<ProductSummaryResponse>> listProducts(@PathVariable UUID vendorId,
                                                                            @RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(required = false) ProductStatus status,
                                                                            @RequestParam(required = false) String keyword) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(vendorProductService.listProducts(effectiveVendorId, status, keyword, pageable));
    }

    @PostMapping("/{productId}/shelve")
    public ApiResponse<ProductResponse> shelve(@PathVariable UUID vendorId,
                                                @PathVariable UUID productId,
                                                @Valid @RequestBody ProductShelveRequest request) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.changeShelveStatus(effectiveVendorId, productId, request));
    }

    @GetMapping("/{productId}/rental-plans")
    public ApiResponse<List<RentalPlanResponse>> listPlans(@PathVariable UUID vendorId,
                                                           @PathVariable UUID productId) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.listPlans(effectiveVendorId, productId));
    }

    @PostMapping("/{productId}/rental-plans")
    public ApiResponse<RentalPlanResponse> createRentalPlan(@PathVariable UUID vendorId,
                                                             @PathVariable UUID productId,
                                                             @Valid @RequestBody RentalPlanRequest request) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.createPlan(effectiveVendorId, productId, request));
    }

    @PutMapping("/{productId}/rental-plans/{planId}")
    public ApiResponse<RentalPlanResponse> updateRentalPlan(@PathVariable UUID vendorId,
                                                             @PathVariable UUID productId,
                                                             @PathVariable UUID planId,
                                                             @Valid @RequestBody RentalPlanRequest request) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.updatePlan(effectiveVendorId, productId, planId, request));
    }

    @PostMapping("/{productId}/rental-plans/{planId}/activate")
    public ApiResponse<RentalPlanResponse> activateRentalPlan(@PathVariable UUID vendorId,
                                                               @PathVariable UUID productId,
                                                               @PathVariable UUID planId) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.activatePlan(effectiveVendorId, productId, planId));
    }

    @PostMapping("/{productId}/rental-plans/{planId}/deactivate")
    public ApiResponse<RentalPlanResponse> deactivateRentalPlan(@PathVariable UUID vendorId,
                                                                 @PathVariable UUID productId,
                                                                 @PathVariable UUID planId) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.deactivatePlan(effectiveVendorId, productId, planId));
    }

    @PostMapping("/{productId}/rental-plans/{planId}/skus")
    public ApiResponse<SkuResponse> createSku(@PathVariable UUID vendorId,
                                               @PathVariable UUID productId,
                                               @PathVariable UUID planId,
                                               @Valid @RequestBody SkuRequest request) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.createSku(effectiveVendorId, productId, planId, request));
    }

    @PutMapping("/{productId}/rental-plans/{planId}/skus/{skuId}")
    public ApiResponse<SkuResponse> updateSku(@PathVariable UUID vendorId,
                                               @PathVariable UUID productId,
                                               @PathVariable UUID planId,
                                               @PathVariable UUID skuId,
                                               @Valid @RequestBody SkuRequest request) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.updateSku(effectiveVendorId, productId, planId, skuId, request));
    }

    @PostMapping("/{productId}/rental-plans/{planId}/skus/{skuId}/inventory/adjust")
    public ApiResponse<SkuResponse> adjustInventory(@PathVariable UUID vendorId,
                                                     @PathVariable UUID productId,
                                                     @PathVariable UUID planId,
                                                     @PathVariable UUID skuId,
                                                     @Valid @RequestBody InventoryAdjustRequest request) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(vendorProductService.adjustInventory(effectiveVendorId, productId, planId, skuId, request));
    }

    @PostMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> uploadCover(@PathVariable UUID vendorId,
                                                       @RequestParam("file") MultipartFile file) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        return ApiResponse.success(productAssetService.uploadCoverImage(file));
    }

    @DeleteMapping("/cover-image")
    public ApiResponse<Void> deleteCover(@PathVariable UUID vendorId,
                                         @RequestParam("fileName") String fileName) {
        UUID effectiveVendorId = resolveVendorId(vendorId);
        productAssetService.deleteTemporaryFile(fileName);
        return ApiResponse.success();
    }

    private UUID resolveVendorId(UUID vendorId) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN")) {
            return vendorId;
        }
        if (!principal.hasRole("VENDOR")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号无权访问厂商资源");
        }
        if (principal.vendorId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
        }
        if (!principal.vendorId().equals(vendorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止访问其他厂商资源");
        }
        return vendorId;
    }
}
