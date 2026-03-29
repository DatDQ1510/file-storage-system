package com.java.file_storage_system.dto.payment;

import com.java.file_storage_system.constant.PaymentProvider;
import com.java.file_storage_system.constant.PaymentTransactionStatus;

import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        String id,
        String tenantId,
        String planId,
        String planName,
        Long amountMinor,
        String currency,
        PaymentProvider provider,
        PaymentTransactionStatus status,
        String providerTransactionId,
        String checkoutUrl,
        Boolean autoRenew,
        String failureReason,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
