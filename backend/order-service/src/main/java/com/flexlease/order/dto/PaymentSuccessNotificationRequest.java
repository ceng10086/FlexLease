package com.flexlease.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * PaymentSuccessNotificationRequest 请求 DTO。
 */
public record PaymentSuccessNotificationRequest(@NotNull UUID transactionId) {
}
