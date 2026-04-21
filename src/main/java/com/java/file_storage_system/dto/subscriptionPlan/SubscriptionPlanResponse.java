package com.java.file_storage_system.dto.subscriptionPlan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.PlanStatus;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionPlanResponse(
        String id,
        String namePlan,
        String description,
        BigInteger baseStorageLimit,
        Integer maxUsers,
        Double price,
        BillingCycle billingCycle,
        PlanStatus planStatus,
        Map<String, Object> features,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
