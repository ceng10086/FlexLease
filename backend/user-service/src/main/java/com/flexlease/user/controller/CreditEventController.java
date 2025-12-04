package com.flexlease.user.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.user.domain.CreditEventType;
import com.flexlease.user.dto.CreditEventRequest;
import com.flexlease.user.dto.UserCreditResponse;
import com.flexlease.user.service.CreditEventService;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/users")
public class CreditEventController {

    private final CreditEventService creditEventService;

    public CreditEventController(CreditEventService creditEventService) {
        this.creditEventService = creditEventService;
    }

    @PostMapping("/{userId}/credit-events")
    public ApiResponse<UserCreditResponse> record(@PathVariable String userId,
                                                  @Valid @RequestBody CreditEventRequest request) {
        if (!SecurityUtils.hasRole("INTERNAL")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅内部服务可记录信用事件");
        }
        UUID userUuid = parseUuid(userId);
        CreditEventType type = parseType(request.eventType());
        UserCreditResponse response = creditEventService.applyEvent(userUuid, type, request.attributes());
        return ApiResponse.success(response);
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "userId 需为合法 UUID");
        }
    }

    private CreditEventType parseType(String raw) {
        try {
            return CreditEventType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法事件类型: " + raw);
        }
    }
}
