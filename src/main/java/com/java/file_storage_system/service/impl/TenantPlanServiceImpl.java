package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.tenantPlan.CreateTenantPlanRequest;
import com.java.file_storage_system.dto.tenantPlan.TenantPlanResponse;
import com.java.file_storage_system.dto.tenantPlan.UpdateTenantPlanRequest;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.TenantPlan;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.SubscriptionPlanRepository;
import com.java.file_storage_system.repository.TenantPlanRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.service.TenantPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantPlanServiceImpl extends BaseServiceImpl<TenantPlan, TenantPlanRepository> implements TenantPlanService {

    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TenantPlanResponse> getAllTenantPlans() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantPlanResponse getTenantPlanById(String tenantPlanId) {
        return mapToResponse(findTenantPlan(tenantPlanId));
    }

    @Override
    @Transactional
    public TenantPlanResponse createTenantPlan(CreateTenantPlanRequest request) {
        validateDateRange(request.planStartDate(), request.planEndDate());

        TenantEntity tenant = findTenant(request.tenantId());
        SubscriptionPlanEntity plan = findPlan(request.planId());

        TenantPlan tenantPlan = new TenantPlan();
        tenantPlan.setStatus(request.status());
        tenantPlan.setPlanStartDate(request.planStartDate());
        tenantPlan.setPlanEndDate(request.planEndDate());
        tenantPlan.setIsAutoRenew(Boolean.TRUE.equals(request.isAutoRenew()));
        tenantPlan.setTenant(tenant);
        tenantPlan.setPlan(plan);

        return mapToResponse(repository.save(tenantPlan));
    }

    @Override
    @Transactional
    public TenantPlanResponse updateTenantPlan(String tenantPlanId, UpdateTenantPlanRequest request) {
        TenantPlan tenantPlan = findTenantPlan(tenantPlanId);
        validateDateRange(request.planStartDate(), request.planEndDate());

        TenantEntity tenant = findTenant(request.tenantId());
        SubscriptionPlanEntity plan = findPlan(request.planId());

        tenantPlan.setStatus(request.status());
        tenantPlan.setPlanStartDate(request.planStartDate());
        tenantPlan.setPlanEndDate(request.planEndDate());
        tenantPlan.setIsAutoRenew(Boolean.TRUE.equals(request.isAutoRenew()));
        tenantPlan.setTenant(tenant);
        tenantPlan.setPlan(plan);

        return mapToResponse(repository.save(tenantPlan));
    }

    @Override
    @Transactional
    public void deleteTenantPlan(String tenantPlanId) {
        if (!repository.existsById(tenantPlanId)) {
            throw ResourceNotFoundException.byField("TenantPlan", "id", tenantPlanId);
        }
        repository.deleteById(tenantPlanId);
    }

    private TenantPlan findTenantPlan(String tenantPlanId) {
        return repository.findById(tenantPlanId)
                .orElseThrow(() -> ResourceNotFoundException.byField("TenantPlan", "id", tenantPlanId));
    }

    private TenantEntity findTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Tenant", "id", tenantId));
    }

    private SubscriptionPlanEntity findPlan(String planId) {
        return subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> ResourceNotFoundException.byField("SubscriptionPlan", "id", planId));
    }

    private void validateDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw ConflictException.withMessage("planEndDate must be after planStartDate");
        }
    }

    private TenantPlanResponse mapToResponse(TenantPlan tenantPlan) {
        SubscriptionPlanEntity plan = tenantPlan.getPlan();
        return new TenantPlanResponse(
                tenantPlan.getId(),
                tenantPlan.getStatus(),
                tenantPlan.getPlanStartDate(),
                tenantPlan.getPlanEndDate(),
                tenantPlan.getIsAutoRenew(),
                tenantPlan.getTenant().getId(),
                plan == null ? null : plan.getId(),
                plan == null ? null : plan.getNamePlan(),
                plan == null ? null : plan.getBillingCycle(),
                tenantPlan.getCreatedAt(),
                tenantPlan.getUpdatedAt()
        );
    }
}
