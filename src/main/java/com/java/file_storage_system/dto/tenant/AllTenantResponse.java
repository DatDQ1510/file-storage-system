package com.java.file_storage_system.dto.tenant;

import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.TenantStatus;
import com.java.file_storage_system.constant.TenantPlanStatus;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record AllTenantResponse(
        String id,
        String nameTenant,
        String domainTenant,
        BigInteger exTraStorageSize,
        BigInteger usedStorageSize,
        TenantStatus statusTenant,
        String tenantAdminId,
        String tenantAdminUserName,
        String tenantAdminEmail,
        String tenantAdminPhoneNumber,
        String planId,
        String planName,
        BigInteger planBaseStorageLimit,
        Double planPrice,
        BillingCycle planBillingCycle,
        TenantPlanStatus tenantPlanStatus,
        LocalDateTime planStartDate,
        LocalDateTime planEndDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
