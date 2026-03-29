package com.java.file_storage_system.entity;

import com.java.file_storage_system.constant.PaymentProvider;
import com.java.file_storage_system.constant.PaymentTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "paymentTransactions",
        indexes = {
                @Index(name = "idx_payment_tenant_id", columnList = "tenantId"),
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_provider_tx", columnList = "providerTransactionId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_provider_transaction_id", columnNames = {"providerTransactionId"})
        }
)
public class PaymentTransactionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenantId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SubscriptionPlanEntity plan;

    @Column(name = "amountMinor", nullable = false)
    private Long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentTransactionStatus status;

    @Column(name = "providerTransactionId")
    private String providerTransactionId;

    @Column(name = "checkoutUrl")
    private String checkoutUrl;

    @Column(name = "paidAt")
    private LocalDateTime paidAt;

    @Column(name = "failureReason")
    private String failureReason;

    @Column(name = "autoRenew", nullable = false)
    private Boolean autoRenew = true;
}
