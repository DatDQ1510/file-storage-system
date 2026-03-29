package com.java.file_storage_system.dto.payment;

import com.java.file_storage_system.constant.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentCheckoutRequest(
        @NotBlank(message = "tenantId is required")
        String tenantId,
        @NotBlank(message = "planId is required")
        String planId,
        @NotNull(message = "provider is required")
        PaymentProvider provider,
        String returnUrl,
        String cancelUrl,
        Boolean autoRenew
) {
}
