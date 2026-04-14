package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.user.allUser.AllUserPageResponse;
import com.java.file_storage_system.dto.user.createUser.CreateTenantUserRequest;
import com.java.file_storage_system.dto.user.createUser.UserCreatedResponse;
import com.java.file_storage_system.dto.user.changePassword.ResetUserPasswordByTenantAdminRequest;
import com.java.file_storage_system.dto.user.searchUser.UserSearchPageResponse;
import com.java.file_storage_system.entity.UserEntity;

public interface UserService extends BaseService<UserEntity> {

	UserCreatedResponse createUserByTenantAdmin(String tenantAdminId, CreateTenantUserRequest request);

	AllUserPageResponse getAllUsersInTenant(String tenantId, int page, int offset);

	void resetUserPasswordByTenantAdmin(String tenantAdminId, String userId, ResetUserPasswordByTenantAdminRequest request);

	UserSearchPageResponse searchUsersInTenant(String tenantId, String keyword, int page, int size);
}
