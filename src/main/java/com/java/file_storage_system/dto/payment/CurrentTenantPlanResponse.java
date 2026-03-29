package com.java.file_storage_system.dto.payment;

import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.TenantPlanStatus;

import java.time.LocalDateTime;

public record CurrentTenantPlanResponse(
        String tenantPlanId,
        String tenantId,
        String planId,
        String planName,
        BillingCycle billingCycle,
        TenantPlanStatus status,
        Boolean autoRenew,
        LocalDateTime planStartDate,
        LocalDateTime planEndDate
) {
}
