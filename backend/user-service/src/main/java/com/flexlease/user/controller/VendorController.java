package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.user.domain.VendorStatus;
import com.flexlease.user.dto.PagedResponse;
import com.flexlease.user.dto.VendorCommissionProfileRequest;
import com.flexlease.user.dto.VendorCommissionProfileResponse;
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
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可查询厂商列表");
        }
        VendorStatus statusEnum = parseStatus(status);
        return ApiResponse.success(vendorService.list(statusEnum, page, size));
    }

    @GetMapping("/{vendorId}")
    public ApiResponse<VendorResponse> getVendor(@PathVariable UUID vendorId) {
        VendorResponse response = vendorService.get(vendorId);
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN")) {
            return ApiResponse.success(response);
        }
        if (!principal.hasRole("VENDOR")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅厂商可查看自身信息");
        }
        ensureVendorAccess(principal, response, "无权查看该厂商信息");
        return ApiResponse.success(response);
    }

    @PutMapping("/{vendorId}")
    public ApiResponse<VendorResponse> updateVendor(@PathVariable UUID vendorId,
                                                    @Valid @RequestBody VendorUpdateRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN")) {
            return ApiResponse.success(vendorService.update(vendorId, request));
        }
        if (!principal.hasRole("VENDOR")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅厂商可更新资料");
        }
        VendorResponse response = vendorService.get(vendorId);
        ensureVendorAccess(principal, response, "无权更新该厂商资料");
        return ApiResponse.success(vendorService.update(vendorId, request));
    }

    @PostMapping("/{vendorId}/suspend")
    public ApiResponse<VendorResponse> updateVendorStatus(@PathVariable UUID vendorId,
                                                          @Valid @RequestBody VendorStatusUpdateRequest request) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可变更厂商状态");
        }
        return ApiResponse.success(vendorService.updateStatus(vendorId, request.status()));
    }

    @PutMapping("/{vendorId}/commission-profile")
    public ApiResponse<VendorCommissionProfileResponse> updateCommissionProfile(@PathVariable UUID vendorId,
                                                                                @Valid @RequestBody VendorCommissionProfileRequest request) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可配置抽成策略");
        }
        return ApiResponse.success(vendorService.updateCommissionProfile(vendorId, request));
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

    private void ensureVendorAccess(FlexleasePrincipal principal, VendorResponse vendor, String forbiddenMessage) {
        UUID principalVendorId = principal.vendorId();
        if (principalVendorId != null) {
            if (principalVendorId.equals(vendor.id())) {
                return;
            }
            throw new BusinessException(ErrorCode.FORBIDDEN, forbiddenMessage);
        }
        UUID principalUserId = principal.userId();
        if (principalUserId != null && principalUserId.equals(vendor.ownerUserId())) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, forbiddenMessage);
    }
}
