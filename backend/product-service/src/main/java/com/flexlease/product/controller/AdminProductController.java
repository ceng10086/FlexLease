package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
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

/**
 * 管理员商品审核接口。
 * <p>
 * 覆盖待审核商品列表、审核通过/驳回等操作。
 */
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
        SecurityUtils.requireRole("ADMIN");
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(productAdminService.listProducts(status, keyword, pageable));
    }

    @PostMapping("/{productId}/approve")
    public ApiResponse<ProductResponse> approve(@PathVariable UUID productId,
                                                 @Valid @RequestBody ProductApprovalRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可审核商品");
        }
        UUID reviewerId = ensureReviewerId(principal);
        return ApiResponse.success(productAdminService.approveProduct(productId, reviewerId, request.remark()));
    }

    @PostMapping("/{productId}/reject")
    public ApiResponse<ProductResponse> reject(@PathVariable UUID productId,
                                                @Valid @RequestBody ProductApprovalRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可审核商品");
        }
        UUID reviewerId = ensureReviewerId(principal);
        return ApiResponse.success(productAdminService.rejectProduct(productId, reviewerId, request.remark()));
    }

    private UUID ensureReviewerId(FlexleasePrincipal principal) {
        if (principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
        }
        return principal.userId();
    }
}
