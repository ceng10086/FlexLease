package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.product.domain.RentalPlanType;
import com.flexlease.product.dto.CatalogProductResponse;
import com.flexlease.product.dto.PagedResponse;
import com.flexlease.product.service.CatalogQueryService;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端商品目录（Catalog）接口。
 * <p>
 * 用于前台列表/详情页展示已上架商品，支持按类目/关键词/租赁类型/押金区间筛选与按租金排序。
 */
@RestController
@RequestMapping("/api/v1/catalog/products")
public class CatalogController {

    private final CatalogQueryService catalogQueryService;

    public CatalogController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<CatalogProductResponse>> list(@RequestParam(required = false) String categoryCode,
                                                                    @RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) RentalPlanType planType,
                                                                    @RequestParam(required = false) BigDecimal minDeposit,
                                                                    @RequestParam(required = false) BigDecimal maxDeposit,
                                                                    @RequestParam(required = false) String rentSort,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedSort = rentSort == null || rentSort.isBlank() ? null : rentSort.trim().toUpperCase(Locale.ROOT);
        return ApiResponse.success(catalogQueryService.listActive(categoryCode, keyword, planType, minDeposit, maxDeposit, normalizedSort, pageable));
    }

    @GetMapping("/{productId}")
    public ApiResponse<CatalogProductResponse> get(@PathVariable UUID productId) {
        return ApiResponse.success(catalogQueryService.getProduct(productId));
    }
}
