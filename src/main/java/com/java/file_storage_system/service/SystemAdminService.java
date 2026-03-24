package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.systemAdmin.create.CreateSystemAdminRequest;
import com.java.file_storage_system.dto.systemAdmin.create.SystemAdminCreatedResponse;
import com.java.file_storage_system.entity.SystemAdminEntity;

public interface SystemAdminService extends BaseService<SystemAdminEntity> {

	SystemAdminCreatedResponse bootstrapSystemAdmin(String bootstrapSecret, CreateSystemAdminRequest request);

	SystemAdminCreatedResponse createSystemAdminBySystemAdmin(CreateSystemAdminRequest request);
}
