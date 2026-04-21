package com.java.file_storage_system.dto.subscriptionPlan;

import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.PlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigInteger;
import java.util.Map;

public record SubscriptionPlanRequest(
        @NotBlank(message = "namePlan is required")
        @Size(max = 255, message = "namePlan must be at most 255 characters")
        String namePlan,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @NotNull(message = "baseStorageLimit is required")
        @Positive(message = "baseStorageLimit must be greater than 0")
        BigInteger baseStorageLimit,

        @NotNull(message = "maxUsers is required")
        @Positive(message = "maxUsers must be greater than 0")
        Integer maxUsers,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must be greater than or equal to 0")
        Double price,

        @NotNull(message = "billingCycle is required")
        BillingCycle billingCycle,

        PlanStatus planStatus,

        Map<String, Object> features
) {
}
