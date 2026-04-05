package com.java.file_storage_system.dto.tenantPlan;

import com.java.file_storage_system.constant.TenantPlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public record CreateTenantPlanRequest(
        @NotNull(message = "status is required")
        TenantPlanStatus status,

        @NotNull(message = "planStartDate is required")
        @PastOrPresent(message = "planStartDate must be in the past or present")
        LocalDateTime planStartDate,

        @NotNull(message = "planEndDate is required")
        LocalDateTime planEndDate,

        @NotNull(message = "isAutoRenew is required")
        Boolean isAutoRenew,

        @NotBlank(message = "tenantId is required")
        String tenantId,

        @NotBlank(message = "planId is required")
        String planId
) {
}