package com.java.file_storage_system.dto.tenantPlan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.TenantPlanStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TenantPlanResponse(
        String id,
        TenantPlanStatus status,
        LocalDateTime planStartDate,
        LocalDateTime planEndDate,
        Boolean isAutoRenew,
        String tenantId,
        String planId,
        String planName,
        BillingCycle billingCycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}