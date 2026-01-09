package com.flexlease.user.dto;

import com.flexlease.common.user.CreditTier;
import java.util.UUID;

/**
 * 用户信用信息响应（用于跨服务/前端快速读取信用分与档位）。
 */
public record UserCreditResponse(
        UUID userId,
        Integer creditScore,
        CreditTier creditTier
) {
}
