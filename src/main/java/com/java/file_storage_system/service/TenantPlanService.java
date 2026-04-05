package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.tenantPlan.CreateTenantPlanRequest;
import com.java.file_storage_system.dto.tenantPlan.TenantPlanResponse;
import com.java.file_storage_system.dto.tenantPlan.UpdateTenantPlanRequest;
import com.java.file_storage_system.entity.TenantPlan;

import java.util.List;

public interface TenantPlanService extends BaseService<TenantPlan> {

	List<TenantPlanResponse> getAllTenantPlans();

	TenantPlanResponse getTenantPlanById(String tenantPlanId);

	TenantPlanResponse createTenantPlan(CreateTenantPlanRequest request);

	TenantPlanResponse updateTenantPlan(String tenantPlanId, UpdateTenantPlanRequest request);

	void deleteTenantPlan(String tenantPlanId);
}
