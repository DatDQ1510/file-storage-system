package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    /**
     * MAP các redirect URL theo từng role
     */
    private static final Map<UserRole, String> ROLE_REDIRECTS = new HashMap<>();

    static {
        // SYSTEM_ADMIN - Quản lý hệ thống
        ROLE_REDIRECTS.put(UserRole.SYSTEM_ADMIN, "/dashboard/system");

        // TENANT_ADMIN - Quản lý công ty
        ROLE_REDIRECTS.put(UserRole.TENANT_ADMIN, "/dashboard/tenant");

        // USER - Người dùng bình thường
        ROLE_REDIRECTS.put(UserRole.USER, "/dashboard/user");
    }

    @Override
    public String getRedirectUrlByRole(UserRole role) {
        return ROLE_REDIRECTS.getOrDefault(role, "/dashboard");
    }

    @Override
    public boolean canAccessTenant(CustomUserDetails user, String targetTenantId) {
        UserRole role = UserRole.fromString(user.getRole());
        
        // SYSTEM_ADMIN có quyền truy cập tất cả tenants
        if (role == UserRole.SYSTEM_ADMIN) {
            return true;
        }

        // TENANT_ADMIN và USER chỉ có thể truy cập tenant của họ
        return user.getTenantId() != null && user.getTenantId().equals(targetTenantId);
    }
}
