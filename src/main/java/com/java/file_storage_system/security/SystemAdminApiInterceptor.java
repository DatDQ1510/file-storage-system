package com.java.file_storage_system.security;

import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.repository.SystemAdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SystemAdminApiInterceptor implements HandlerInterceptor {

    public static final String SYSTEM_ADMIN_HEADER = "X-System-Admin-Id";

    private final SystemAdminRepository systemAdminRepository;

    public SystemAdminApiInterceptor(SystemAdminRepository systemAdminRepository) {
        this.systemAdminRepository = systemAdminRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String systemAdminId = request.getHeader(SYSTEM_ADMIN_HEADER);

        if (systemAdminId == null || systemAdminId.isBlank()) {
            throw new ForbiddenException("Missing X-System-Admin-Id header");
        }

        if (!systemAdminRepository.existsById(systemAdminId)) {
            throw new ForbiddenException("Only SystemAdmin accounts can call this API");
        }

        return true;
    }
}