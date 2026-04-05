package com.java.file_storage_system.mapper;

import com.java.file_storage_system.dto.subscriptionPlan.SubscriptionPlanRequest;
import com.java.file_storage_system.dto.subscriptionPlan.SubscriptionPlanResponse;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubscriptionPlanMapper {

    private final ObjectMapper objectMapper;

    public SubscriptionPlanEntity toEntity(SubscriptionPlanRequest request) {
        return toEntity(request, null);
    }

    public SubscriptionPlanEntity toEntity(SubscriptionPlanRequest request, String id) {
        SubscriptionPlanEntity entity = new SubscriptionPlanEntity();
        entity.setId(id);
        entity.setNamePlan(request.namePlan());
        entity.setDescription(request.description());
        entity.setBaseStorageLimit(request.baseStorageLimit());
        entity.setMaxUsers(request.maxUsers());
        entity.setPrice(request.price());
        entity.setBillingCycle(request.billingCycle());
        entity.setFeatures(request.features() == null ? null : objectMapper.valueToTree(request.features()));
        return entity;
    }

    public SubscriptionPlanResponse toResponse(SubscriptionPlanEntity entity) {
        return new SubscriptionPlanResponse(
                entity.getId(),
                entity.getNamePlan(),
                entity.getDescription(),
                entity.getBaseStorageLimit(),
                entity.getMaxUsers(),
                entity.getPrice(),
                entity.getBillingCycle(),
                entity.getFeatures() == null ? null : objectMapper.convertValue(entity.getFeatures(), Map.class),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<SubscriptionPlanResponse> toResponses(List<SubscriptionPlanEntity> entities) {
        return entities.stream().map(this::toResponse).toList();
    }
}