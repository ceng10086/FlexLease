package com.flexlease.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateUserVendorRequest(@NotNull UUID vendorId) {
}
