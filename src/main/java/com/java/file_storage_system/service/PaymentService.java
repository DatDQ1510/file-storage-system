package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.payment.CurrentTenantPlanResponse;
import com.java.file_storage_system.dto.payment.PaymentCheckoutRequest;
import com.java.file_storage_system.dto.payment.PaymentCheckoutResponse;
import com.java.file_storage_system.dto.payment.PaymentConfirmRequest;
import com.java.file_storage_system.dto.payment.PaymentTransactionResponse;
import com.java.file_storage_system.dto.payment.PaymentWebhookRequest;

import java.util.List;

public interface PaymentService {

    PaymentCheckoutResponse createCheckout(PaymentCheckoutRequest request);

    PaymentTransactionResponse confirmPayment(String transactionId, PaymentConfirmRequest request);

    PaymentTransactionResponse handleWebhook(PaymentWebhookRequest request);

    PaymentTransactionResponse getTransaction(String transactionId);

    List<PaymentTransactionResponse> getTenantTransactions(String tenantId);

    CurrentTenantPlanResponse getCurrentTenantPlan(String tenantId);
}
