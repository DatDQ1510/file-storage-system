package com.java.file_storage_system.security;

import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.repository.TenantAdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantAdminApiInterceptor implements HandlerInterceptor {

    public static final String TENANT_ADMIN_HEADER = "X-Tenant-Admin-Id";

    private final TenantAdminRepository tenantAdminRepository;

    public TenantAdminApiInterceptor(TenantAdminRepository tenantAdminRepository) {
        this.tenantAdminRepository = tenantAdminRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantAdminId = request.getHeader(TENANT_ADMIN_HEADER);

        if (tenantAdminId == null || tenantAdminId.isBlank()) {
            throw new ForbiddenException("Missing X-Tenant-Admin-Id header");
        }

        if (!tenantAdminRepository.existsById(tenantAdminId)) {
            throw new ForbiddenException("Only TenantAdmin accounts can call this API");
        }

        return true;
    }
}
