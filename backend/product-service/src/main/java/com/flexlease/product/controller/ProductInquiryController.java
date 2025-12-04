package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.product.dto.ProductInquiryRequest;
import com.flexlease.product.dto.ProductInquiryResponse;
import com.flexlease.product.service.ProductInquiryService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/products/{productId}/inquiries")
public class ProductInquiryController {

    private final ProductInquiryService productInquiryService;

    public ProductInquiryController(ProductInquiryService productInquiryService) {
        this.productInquiryService = productInquiryService;
    }

    @PostMapping
    public ApiResponse<ProductInquiryResponse> create(@PathVariable UUID productId,
                                                      @Valid @RequestBody ProductInquiryRequest request) {
        if (!SecurityUtils.hasRole("USER") && !SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅消费者可提交咨询");
        }
        UUID requesterId = SecurityUtils.requireUserId();
        return ApiResponse.success(productInquiryService.createInquiry(productId, requesterId, request));
    }
}
