package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.domain.VendorApplicationStatus;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ApiResponse<VendorApplicationResponse> submit(@RequestHeader("X-User-Id") String ownerId,
                                                         @Valid @RequestBody VendorApplicationRequest request) {
        UUID ownerUuid = parseUuid(ownerId, "ownerId");
        VendorApplicationResponse response = vendorApplicationService.submit(ownerUuid, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<VendorApplicationResponse> detail(@PathVariable String id) {
        VendorApplicationResponse response = vendorApplicationService.get(parseUuid(id, "id"));
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<List<VendorApplicationResponse>> list(@RequestParam(required = false) String status) {
        VendorApplicationStatus statusEnum = null;
        if (StringUtils.hasText(status)) {
            try {
                statusEnum = VendorApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + status);
            }
        }
        return ApiResponse.success(vendorApplicationService.list(statusEnum));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<VendorApplicationResponse> approve(@PathVariable String id,
                                                          @Valid @RequestBody VendorApplicationReviewRequest request) {
        VendorApplicationResponse response = vendorApplicationService.approve(
                parseUuid(id, "id"),
                parseUuid(request.reviewerId(), "reviewerId"),
                request.remark()
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<VendorApplicationResponse> reject(@PathVariable String id,
                                                         @Valid @RequestBody VendorApplicationReviewRequest request) {
        VendorApplicationResponse response = vendorApplicationService.reject(
                parseUuid(id, "id"),
                parseUuid(request.reviewerId(), "reviewerId"),
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
}
