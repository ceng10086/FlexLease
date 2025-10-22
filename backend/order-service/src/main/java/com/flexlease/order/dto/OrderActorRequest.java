package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderActorRequest(@NotNull UUID actorId) {
}
