package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.user.createUser.CreateTenantUserRequest;
import com.java.file_storage_system.dto.user.createUser.UserCreatedResponse;
import com.java.file_storage_system.entity.UserEntity;

public interface UserService extends BaseService<UserEntity> {

	UserCreatedResponse createUserByTenantAdmin(String tenantAdminId, CreateTenantUserRequest request);
}
