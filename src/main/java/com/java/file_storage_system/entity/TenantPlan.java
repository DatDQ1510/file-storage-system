package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tenantPlans"
)
public class TenantPlan extends BaseEntity {

    @Column(name = "status")
    private String status;

    @Column(name = "planStartDate")
    private LocalDateTime planStartDate;

    @Column(name = "planEndDate")
    private LocalDateTime planEndDate;

    @Column(name = "isAutoRenew", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isAutoRenew;

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

}
