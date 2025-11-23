package com.flexlease.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OrderMessageRequest(
        @NotNull UUID actorId,
        @NotBlank @Size(max = 500) String message
) {
}
