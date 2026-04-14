package com.java.file_storage_system.dto.systemAdmin.initial;

import java.time.LocalDateTime;

public record InitialTenantSetupResponse(
        String tenantId,
        String tenantDomain,
        String tenantAdminId,
        String tenantAdminUserName,
        String tenantPlanId,
        String planId,
        LocalDateTime tenantPlanStartDate,
        LocalDateTime tenantPlanEndDate
    ) {
}
