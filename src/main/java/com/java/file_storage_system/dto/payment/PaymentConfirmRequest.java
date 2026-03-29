package com.java.file_storage_system.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequest(
        @NotBlank(message = "providerTransactionId is required")
        String providerTransactionId,
        @NotNull(message = "success is required")
        Boolean success,
        String failureReason
) {
}
