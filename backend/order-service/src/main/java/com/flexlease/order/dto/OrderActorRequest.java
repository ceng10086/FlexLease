package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * OrderActorRequest 请求 DTO。
 */
public record OrderActorRequest(@NotNull UUID actorId) {
}
