package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.tenantAdmin.check.CheckTenantAdminRequest;
import com.java.file_storage_system.dto.tenantAdmin.check.CheckTenantAdminResponse;
import com.java.file_storage_system.dto.tenantAdmin.create.CreateTenantAdminRequest;
import com.java.file_storage_system.dto.tenantAdmin.create.TenantAdminCreatedResponse;
import com.java.file_storage_system.entity.TenantAdminEntity;

public interface TenantAdminService extends BaseService<TenantAdminEntity> {

	TenantAdminCreatedResponse createTenantAdminBySystemAdmin(CreateTenantAdminRequest request);

	CheckTenantAdminResponse checkTenantAdmin(CheckTenantAdminRequest request);

	CheckTenantAdminResponse checkTenantAdmin(String username, String email, String sdt);
}
