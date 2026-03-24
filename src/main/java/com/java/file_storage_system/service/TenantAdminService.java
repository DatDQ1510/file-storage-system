package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.tenantAdmin.create.CreateTenantAdminRequest;
import com.java.file_storage_system.dto.tenantAdmin.create.TenantAdminCreatedResponse;
import com.java.file_storage_system.entity.TenantAdminEntity;

public interface TenantAdminService extends BaseService<TenantAdminEntity> {

	TenantAdminCreatedResponse createTenantAdminBySystemAdmin(CreateTenantAdminRequest request);
}
