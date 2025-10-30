package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.domain.VendorStatus;
import com.flexlease.user.dto.PagedResponse;
import com.flexlease.user.dto.VendorResponse;
import com.flexlease.user.dto.VendorStatusUpdateRequest;
import com.flexlease.user.dto.VendorUpdateRequest;
import com.flexlease.user.service.VendorService;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<VendorResponse>> listVendors(@RequestParam(required = false) String status,
                                                                  @RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        VendorStatus statusEnum = parseStatus(status);
        return ApiResponse.success(vendorService.list(statusEnum, page, size));
    }

    @GetMapping("/{vendorId}")
    public ApiResponse<VendorResponse> getVendor(@PathVariable UUID vendorId) {
        return ApiResponse.success(vendorService.get(vendorId));
    }

    @PutMapping("/{vendorId}")
    public ApiResponse<VendorResponse> updateVendor(@PathVariable UUID vendorId,
                                                    @Valid @RequestBody VendorUpdateRequest request) {
        return ApiResponse.success(vendorService.update(vendorId, request));
    }

    @PostMapping("/{vendorId}/suspend")
    public ApiResponse<VendorResponse> updateVendorStatus(@PathVariable UUID vendorId,
                                                          @Valid @RequestBody VendorStatusUpdateRequest request) {
        return ApiResponse.success(vendorService.updateStatus(vendorId, request.status()));
    }

    private VendorStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return VendorStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + status);
        }
    }
}
