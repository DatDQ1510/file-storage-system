package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.payment.CurrentTenantPlanResponse;
import com.java.file_storage_system.dto.payment.PaymentCheckoutRequest;
import com.java.file_storage_system.dto.payment.PaymentCheckoutResponse;
import com.java.file_storage_system.dto.payment.PaymentConfirmRequest;
import com.java.file_storage_system.dto.payment.PaymentTransactionResponse;
import com.java.file_storage_system.dto.payment.PaymentWebhookRequest;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<PaymentCheckoutResponse>> createCheckout(
            @Valid @RequestBody PaymentCheckoutRequest request,
            HttpServletRequest httpServletRequest
    ) {
        PaymentCheckoutResponse response = paymentService.createCheckout(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create payment checkout successfully", response, httpServletRequest.getRequestURI()));
    }

    @PostMapping("/transactions/{transactionId}/confirm")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> confirmPayment(
            @PathVariable("transactionId") String transactionId,
            @Valid @RequestBody PaymentConfirmRequest request,
            HttpServletRequest httpServletRequest
    ) {
        PaymentTransactionResponse response = paymentService.confirmPayment(transactionId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Confirm payment successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> handleWebhook(
            @Valid @RequestBody PaymentWebhookRequest request,
            HttpServletRequest httpServletRequest
    ) {
        PaymentTransactionResponse response = paymentService.handleWebhook(request);

        return ResponseEntity.ok(
                ApiResponse.success("Handle payment webhook successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> getTransaction(
            @PathVariable("transactionId") String transactionId,
            HttpServletRequest httpServletRequest
    ) {
        PaymentTransactionResponse response = paymentService.getTransaction(transactionId);

        return ResponseEntity.ok(
                ApiResponse.success("Get payment transaction successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @GetMapping("/tenants/{tenantId}/transactions")
    public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getTenantTransactions(
            @PathVariable("tenantId") String tenantId,
            HttpServletRequest httpServletRequest
    ) {
        List<PaymentTransactionResponse> response = paymentService.getTenantTransactions(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success("Get tenant payment transactions successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @GetMapping("/tenants/{tenantId}/current-plan")
    public ResponseEntity<ApiResponse<CurrentTenantPlanResponse>> getCurrentTenantPlan(
            @PathVariable("tenantId") String tenantId,
            HttpServletRequest httpServletRequest
    ) {
        CurrentTenantPlanResponse response = paymentService.getCurrentTenantPlan(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success("Get current tenant plan successfully", response, httpServletRequest.getRequestURI())
        );
    }
}
