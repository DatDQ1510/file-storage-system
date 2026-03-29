package com.java.file_storage_system.dto.payment;

import com.java.file_storage_system.constant.PaymentProvider;
import com.java.file_storage_system.constant.PaymentTransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentWebhookRequest(
        @NotBlank(message = "providerTransactionId is required")
        String providerTransactionId,
        @NotNull(message = "provider is required")
        PaymentProvider provider,
        @NotNull(message = "status is required")
        PaymentTransactionStatus status,
        String signature
) {
}
