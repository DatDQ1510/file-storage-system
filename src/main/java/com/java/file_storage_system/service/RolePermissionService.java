package com.java.file_storage_system.service;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.custom.CustomUserDetails;

public interface RolePermissionService {

    /**
     * Lấy URL redirect dựa trên role
     */
    String getRedirectUrlByRole(UserRole role);

    /**
     * Kiểm tra xem 2 tenant IDs có khớp nhau không (cho phép null cho SYSTEM_ADMIN)
     */
    boolean canAccessTenant(CustomUserDetails user, String targetTenantId);
}
