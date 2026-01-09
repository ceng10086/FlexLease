package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.user.dto.VendorCommissionProfileResponse;
import com.flexlease.user.service.VendorService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 厂商内部接口（供微服务之间调用）。
 * <p>
 * 主要提供抽成配置等信息给订单/支付等服务使用；内部接口要求 INTERNAL 角色。
 */
@RestController
@RequestMapping("/api/v1/internal/vendors")
public class InternalVendorController {

    private final VendorService vendorService;

    public InternalVendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping("/{vendorId}/commission-profile")
    public ApiResponse<VendorCommissionProfileResponse> getCommissionProfile(@PathVariable String vendorId) {
        if (!SecurityUtils.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅内部服务可访问厂商抽成配置");
        }
        UUID vendorUuid = parseUuid(vendorId);
        return ApiResponse.success(vendorService.loadCommissionProfile(vendorUuid));
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "vendorId 需为合法 UUID");
        }
    }
}
