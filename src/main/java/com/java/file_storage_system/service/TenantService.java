package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.tenant.AllTenantPageResponse;
import com.java.file_storage_system.dto.tenant.CreateTenantRequest;
import com.java.file_storage_system.dto.tenant.UpdateTenantRequest;
import com.java.file_storage_system.entity.TenantEntity;

public interface TenantService extends BaseService<TenantEntity> {

	TenantEntity createTenant(CreateTenantRequest request);

	TenantEntity updateTenant(String tenantId, UpdateTenantRequest request);

	AllTenantPageResponse getAllTenants(int page, int offset);

	boolean existsByDomainTenant(String domainTenant);
}
