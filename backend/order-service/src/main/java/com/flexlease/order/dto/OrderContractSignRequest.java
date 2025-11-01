package com.flexlease.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderContractSignRequest(
        @NotNull UUID userId,
        @NotBlank(message = "签名内容不能为空") String signature
) {
}
