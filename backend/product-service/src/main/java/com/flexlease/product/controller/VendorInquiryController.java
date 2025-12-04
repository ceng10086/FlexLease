package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.product.domain.ProductInquiryStatus;
import com.flexlease.product.dto.ProductInquiryReplyRequest;
import com.flexlease.product.dto.ProductInquiryResponse;
import com.flexlease.product.service.ProductInquiryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/inquiries")
public class VendorInquiryController {

    private final ProductInquiryService productInquiryService;

    public VendorInquiryController(ProductInquiryService productInquiryService) {
        this.productInquiryService = productInquiryService;
    }

    @GetMapping
    public ApiResponse<List<ProductInquiryResponse>> list(@PathVariable UUID vendorId,
                                                          @RequestParam(required = false) String status) {
        ensureVendorAccess(vendorId);
        ProductInquiryStatus parsedStatus = parseStatus(status);
        return ApiResponse.success(productInquiryService.listByVendor(vendorId, parsedStatus));
    }

    @PostMapping("/{inquiryId}/reply")
    public ApiResponse<ProductInquiryResponse> reply(@PathVariable UUID vendorId,
                                                     @PathVariable UUID inquiryId,
                                                     @Valid @RequestBody ProductInquiryReplyRequest request) {
        ensureVendorAccess(vendorId);
        return ApiResponse.success(productInquiryService.reply(vendorId, inquiryId, request));
    }

    private void ensureVendorAccess(UUID vendorId) {
        if (!SecurityUtils.hasRole("VENDOR")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅厂商可查看咨询");
        }
        UUID principalVendorId = SecurityUtils.requirePrincipal().vendorId();
        if (principalVendorId == null || !principalVendorId.equals(vendorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看其他厂商咨询");
        }
    }

    private ProductInquiryStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ProductInquiryStatus.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + raw);
        }
    }
}
