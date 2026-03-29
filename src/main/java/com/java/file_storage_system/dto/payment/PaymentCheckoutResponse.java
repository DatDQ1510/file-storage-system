package com.java.file_storage_system.dto.payment;

import com.java.file_storage_system.constant.PaymentProvider;
import com.java.file_storage_system.constant.PaymentTransactionStatus;

public record PaymentCheckoutResponse(
        String transactionId,
        PaymentTransactionStatus status,
        PaymentProvider provider,
        String checkoutUrl,
        Long amountMinor,
        String currency
) {
}
