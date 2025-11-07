package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PaymentSuccessNotificationRequest(@NotNull UUID transactionId) {
}
