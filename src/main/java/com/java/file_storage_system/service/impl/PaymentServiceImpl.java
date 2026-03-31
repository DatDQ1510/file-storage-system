package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.PaymentTransactionStatus;
import com.java.file_storage_system.constant.TenantPlanStatus;
import com.java.file_storage_system.dto.payment.CurrentTenantPlanResponse;
import com.java.file_storage_system.dto.payment.PaymentCheckoutRequest;
import com.java.file_storage_system.dto.payment.PaymentCheckoutResponse;
import com.java.file_storage_system.dto.payment.PaymentConfirmRequest;
import com.java.file_storage_system.dto.payment.PaymentTransactionResponse;
import com.java.file_storage_system.dto.payment.PaymentWebhookRequest;
import com.java.file_storage_system.entity.PaymentTransactionEntity;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.TenantPlan;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.PaymentTransactionRepository;
import com.java.file_storage_system.repository.SubscriptionPlanRepository;
import com.java.file_storage_system.repository.TenantPlanRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String DEFAULT_CURRENCY = "USD";

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TenantPlanRepository tenantPlanRepository;

    @Value("${app.payment.webhook-secret:}")
    private String webhookSecret;

    @Override
    @Transactional
    public PaymentCheckoutResponse createCheckout(PaymentCheckoutRequest request) {
        TenantEntity tenant = findTenant(request.tenantId());
        SubscriptionPlanEntity plan = findPlan(request.planId());

        PaymentTransactionEntity transaction = new PaymentTransactionEntity();
        transaction.setTenant(tenant);
        transaction.setPlan(plan);
        transaction.setAmountMinor(convertToMinorUnits(plan.getPrice()));
        transaction.setCurrency(DEFAULT_CURRENCY);
        transaction.setProvider(request.provider());
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        transaction.setAutoRenew(request.autoRenew() == null ? Boolean.TRUE : request.autoRenew());

        PaymentTransactionEntity saved = paymentTransactionRepository.save(transaction);
        String checkoutUrl = createProviderCheckoutSession(saved, request.returnUrl(), request.cancelUrl());
        saved.setCheckoutUrl(checkoutUrl);

        return mapCheckoutResponse(paymentTransactionRepository.save(saved));
    }

    @Override
    @Transactional
    public PaymentTransactionResponse confirmPayment(String transactionId, PaymentConfirmRequest request) {
        PaymentTransactionEntity transaction = findTransaction(transactionId);

        if (transaction.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return mapTransactionResponse(transaction);
        }

        transaction.setProviderTransactionId(request.providerTransactionId());

        if (Boolean.TRUE.equals(request.success())) {
            markTransactionAsSuccess(transaction);
            provisionTenantPlanAfterPayment(transaction);
        } else {
            markTransactionAsFailed(transaction, request.failureReason());
        }

        PaymentTransactionEntity saved = paymentTransactionRepository.save(transaction);
        return mapTransactionResponse(saved);
    }

    @Override
    @Transactional
    public PaymentTransactionResponse handleWebhook(PaymentWebhookRequest request) {
        if (!verifyWebhookSignature(request)) {
            throw new ForbiddenException("Invalid webhook signature");
        }

        PaymentTransactionEntity transaction = paymentTransactionRepository
                .findByProviderTransactionId(request.providerTransactionId())
                .orElseThrow(() -> ResourceNotFoundException.byField("PaymentTransaction", "providerTransactionId", request.providerTransactionId()));

        if (transaction.getProvider() != request.provider()) {
            throw new ConflictException("Webhook provider does not match transaction provider");
        }

        // Webhooks are frequently retried by providers; keep this flow idempotent.
        if (transaction.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return mapTransactionResponse(transaction);
        }

        if (request.status() == PaymentTransactionStatus.SUCCESS) {
            markTransactionAsSuccess(transaction);
            provisionTenantPlanAfterPayment(transaction);
        } else if (request.status() == PaymentTransactionStatus.FAILED) {
            markTransactionAsFailed(transaction, "Provider webhook marked payment as failed");
        } else if (request.status() == PaymentTransactionStatus.CANCELED) {
            transaction.setStatus(PaymentTransactionStatus.CANCELED);
            transaction.setFailureReason("Payment canceled by user/provider");
        }

        PaymentTransactionEntity saved = paymentTransactionRepository.save(transaction);
        return mapTransactionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getTransaction(String transactionId) {
        return mapTransactionResponse(findTransaction(transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getTenantTransactions(String tenantId) {
        findTenant(tenantId);

        return paymentTransactionRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::mapTransactionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentTenantPlanResponse getCurrentTenantPlan(String tenantId) {
        findTenant(tenantId);

        TenantPlan tenantPlan = tenantPlanRepository.findLatestByTenantIdAndStatus(tenantId, TenantPlanStatus.ACTIVE)
                .orElseThrow(() -> ResourceNotFoundException.withMessage("No active plan found for tenant: " + tenantId));

        return mapCurrentPlanResponse(tenantPlan);
    }

    private TenantEntity findTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Tenant", "id", tenantId));
    }

    private SubscriptionPlanEntity findPlan(String planId) {
        return subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> ResourceNotFoundException.byField("SubscriptionPlan", "id", planId));
    }

    private PaymentTransactionEntity findTransaction(String transactionId) {
        return paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> ResourceNotFoundException.byField("PaymentTransaction", "id", transactionId));
    }

    private void markTransactionAsSuccess(PaymentTransactionEntity transaction) {
        transaction.setStatus(PaymentTransactionStatus.SUCCESS);
        transaction.setPaidAt(LocalDateTime.now());
        transaction.setFailureReason(null);
    }

    private void markTransactionAsFailed(PaymentTransactionEntity transaction, String failureReason) {
        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setFailureReason((failureReason == null || failureReason.isBlank())
                ? "Payment failed"
                : failureReason.trim());
    }

    /**
     * SPECIAL PROCESSING METHOD:
     * Replace this method with real gateway session creation (Stripe/PayPal/VNPay, ...).
     */
    private String createProviderCheckoutSession(PaymentTransactionEntity transaction, String returnUrl, String cancelUrl) {
        String successUrl = (returnUrl == null || returnUrl.isBlank()) ? "https://example.com/payment/success" : returnUrl;
        String failUrl = (cancelUrl == null || cancelUrl.isBlank()) ? "https://example.com/payment/cancel" : cancelUrl;

        return "https://payment.local/checkout?tx=" + transaction.getId()
                + "&provider=" + transaction.getProvider().name()
                + "&returnUrl=" + successUrl
                + "&cancelUrl=" + failUrl;
    }

    /**
     * SPECIAL PROCESSING METHOD:
     * Replace with provider signature verification logic.
     */
    private boolean verifyWebhookSignature(PaymentWebhookRequest request) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }

        String signature = request.signature();
        if (signature == null || signature.isBlank()) {
            return false;
        }

        return Objects.equals(webhookSecret, signature.trim());
    }

    /**
     * SPECIAL PROCESSING METHOD:
     * Core billing provisioning flow after successful payment.
     */
    private void provisionTenantPlanAfterPayment(PaymentTransactionEntity transaction) {
        markExistingActivePlanAsExpired(transaction.getTenant().getId());

        TenantPlan tenantPlan = new TenantPlan();
        tenantPlan.setTenant(transaction.getTenant());
        tenantPlan.setPlan(transaction.getPlan());
        tenantPlan.setStatus(TenantPlanStatus.ACTIVE);
        tenantPlan.setPlanStartDate(LocalDateTime.now());
        tenantPlan.setPlanEndDate(calculatePlanEndDate(transaction.getPlan().getBillingCycle()));
        tenantPlan.setIsAutoRenew(Boolean.TRUE.equals(transaction.getAutoRenew()));

        tenantPlanRepository.save(tenantPlan);
    }

    private void markExistingActivePlanAsExpired(String tenantId) {
        tenantPlanRepository.findLatestByTenantIdAndStatus(tenantId, TenantPlanStatus.ACTIVE)
                .ifPresent(activePlan -> {
                    activePlan.setStatus(TenantPlanStatus.EXPIRED);
                    tenantPlanRepository.save(activePlan);
                });
    }

    private LocalDateTime calculatePlanEndDate(BillingCycle billingCycle) {
        LocalDateTime now = LocalDateTime.now();
        return switch (billingCycle) {
            case MONTHLY -> now.plusMonths(1);
            case QUARTERLY -> now.plusMonths(3);
            case YEARLY -> now.plusYears(1);
        };
    }

    private long convertToMinorUnits(Double amount) {
        if (amount == null || amount < 0) {
            throw ConflictException.withMessage("Invalid plan price for payment");
        }
        return Math.round(amount * 100);
    }

    private PaymentCheckoutResponse mapCheckoutResponse(PaymentTransactionEntity entity) {
        return new PaymentCheckoutResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getProvider(),
                entity.getCheckoutUrl(),
                entity.getAmountMinor(),
                entity.getCurrency()
        );
    }

    private PaymentTransactionResponse mapTransactionResponse(PaymentTransactionEntity entity) {
        return new PaymentTransactionResponse(
                entity.getId(),
                entity.getTenant().getId(),
                entity.getPlan().getId(),
                entity.getPlan().getNamePlan(),
                entity.getAmountMinor(),
                entity.getCurrency(),
                entity.getProvider(),
                entity.getStatus(),
                entity.getProviderTransactionId(),
                entity.getCheckoutUrl(),
                entity.getAutoRenew(),
                entity.getFailureReason(),
                entity.getPaidAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CurrentTenantPlanResponse mapCurrentPlanResponse(TenantPlan tenantPlan) {
        return new CurrentTenantPlanResponse(
                tenantPlan.getId(),
                tenantPlan.getTenant().getId(),
                tenantPlan.getPlan().getId(),
                tenantPlan.getPlan().getNamePlan(),
                tenantPlan.getPlan().getBillingCycle(),
                tenantPlan.getStatus(),
                tenantPlan.getIsAutoRenew(),
                tenantPlan.getPlanStartDate(),
                tenantPlan.getPlanEndDate()
        );
    }
}
