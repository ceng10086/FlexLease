package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.dto.PagedResponse;
import com.flexlease.product.dto.ProductApprovalRequest;
import com.flexlease.product.dto.ProductResponse;
import com.flexlease.product.dto.ProductSummaryResponse;
import com.flexlease.product.service.ProductAdminService;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductAdminService productAdminService;

    public AdminProductController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<ProductSummaryResponse>> listProducts(@RequestParam(required = false) ProductStatus status,
                                                                            @RequestParam(defaultValue = "") String keyword,
                                                                            @RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(productAdminService.listProducts(status, keyword, pageable));
    }

    @PostMapping("/{productId}/approve")
    public ApiResponse<ProductResponse> approve(@PathVariable UUID productId,
                                                 @Valid @RequestBody ProductApprovalRequest request) {
        UUID reviewerId = parseReviewerId(request.reviewerId());
        return ApiResponse.success(productAdminService.approveProduct(productId, reviewerId, request.remark()))
                ;
    }

    @PostMapping("/{productId}/reject")
    public ApiResponse<ProductResponse> reject(@PathVariable UUID productId,
                                                @Valid @RequestBody ProductApprovalRequest request) {
        return ApiResponse.success(productAdminService.rejectProduct(productId, request.remark()));
    }

    private UUID parseReviewerId(String reviewerId) {
        try {
            return UUID.fromString(reviewerId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reviewerId 需为合法 UUID");
        }
    }
}
