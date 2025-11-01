package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.domain.VendorApplicationStatus;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.user.dto.VendorApplicationRequest;
import com.flexlease.user.dto.VendorApplicationResponse;
import com.flexlease.user.dto.VendorApplicationReviewRequest;
import com.flexlease.user.service.VendorApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/applications")
public class VendorApplicationController {

    private final VendorApplicationService vendorApplicationService;

    public VendorApplicationController(VendorApplicationService vendorApplicationService) {
        this.vendorApplicationService = vendorApplicationService;
    }

    @PostMapping
    public ApiResponse<VendorApplicationResponse> submit(@Valid @RequestBody VendorApplicationRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("VENDOR")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅厂商账号可提交入驻申请");
        }
        if (principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
        }
        VendorApplicationResponse response = vendorApplicationService.submit(principal.userId(), request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<VendorApplicationResponse> detail(@PathVariable String id) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        VendorApplicationResponse response = vendorApplicationService.get(parseUuid(id, "id"));
        if (!principal.hasRole("ADMIN")) {
            if (!principal.hasRole("VENDOR")) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅厂商可查看申请详情");
            }
            if (principal.userId() == null || !principal.userId().equals(response.ownerUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该申请");
            }
        }
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<List<VendorApplicationResponse>> list(@RequestParam(required = false) String status) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        VendorApplicationStatus statusEnum = null;
        if (StringUtils.hasText(status)) {
            try {
                statusEnum = VendorApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + status);
            }
        }
        if (principal.hasRole("ADMIN")) {
            return ApiResponse.success(vendorApplicationService.list(statusEnum));
        }
        if (!principal.hasRole("VENDOR")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅厂商可查看自身申请");
        }
        if (principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
        }
        return ApiResponse.success(vendorApplicationService.listForOwner(principal.userId(), statusEnum));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<VendorApplicationResponse> approve(@PathVariable String id,
                                                          @Valid @RequestBody VendorApplicationReviewRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可审核厂商申请");
        }
        UUID reviewerId = ensureReviewerId(principal);
        VendorApplicationResponse response = vendorApplicationService.approve(
                parseUuid(id, "id"),
                reviewerId,
                request.remark()
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<VendorApplicationResponse> reject(@PathVariable String id,
                                                         @Valid @RequestBody VendorApplicationReviewRequest request) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (!principal.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可审核厂商申请");
        }
        UUID reviewerId = ensureReviewerId(principal);
        VendorApplicationResponse response = vendorApplicationService.reject(
                parseUuid(id, "id"),
                reviewerId,
                request.remark()
        );
        return ApiResponse.success(response);
    }

    private UUID parseUuid(String raw, String fieldName) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " 需为合法 UUID");
        }
    }

    private UUID ensureReviewerId(FlexleasePrincipal principal) {
        if (principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少用户标识");
        }
        return principal.userId();
    }
}
