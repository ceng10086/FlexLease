package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OrderReturnCompleteRequest(
        @NotNull UUID vendorId,
        @Size(max = 500) String remark
) {
}
