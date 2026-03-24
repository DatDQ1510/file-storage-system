package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.systemAdmin.create.CreateSystemAdminRequest;
import com.java.file_storage_system.dto.systemAdmin.create.SystemAdminCreatedResponse;
import com.java.file_storage_system.entity.SystemAdminEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.repository.SystemAdminRepository;
import com.java.file_storage_system.service.SystemAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SystemAdminServiceImpl extends BaseServiceImpl<SystemAdminEntity, SystemAdminRepository> implements SystemAdminService {

    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.system-admin-bootstrap-secret:}")
    private String configuredBootstrapSecret;

    @Override
    @Transactional
    public SystemAdminCreatedResponse bootstrapSystemAdmin(String bootstrapSecret, CreateSystemAdminRequest request) {
        validateBootstrapSecret(bootstrapSecret);

        if (repository.count() > 0) {
            throw new ForbiddenException("Bootstrap endpoint is disabled because system admin already exists");
        }

        return createSystemAdminInternal(request);
    }

    @Override
    @Transactional
    public SystemAdminCreatedResponse createSystemAdminBySystemAdmin(CreateSystemAdminRequest request) {
        return createSystemAdminInternal(request);
    }

    private void validateBootstrapSecret(String bootstrapSecret) {
        if (configuredBootstrapSecret == null || configuredBootstrapSecret.isBlank()) {
            throw new ForbiddenException("System admin bootstrap secret is not configured");
        }

        if (bootstrapSecret == null || bootstrapSecret.isBlank()) {
            throw new ForbiddenException("Missing X-System-Admin-Bootstrap-Secret header");
        }

        if (!Objects.equals(configuredBootstrapSecret, bootstrapSecret)) {
            throw new ForbiddenException("Invalid bootstrap secret");
        }
    }

    private SystemAdminCreatedResponse createSystemAdminInternal(CreateSystemAdminRequest request) {
        String normalizedUserName = request.getUserName().trim().toLowerCase();

        if (repository.existsByUsernameIgnoreCase(normalizedUserName)) {
            throw new ConflictException("System admin username already exists: " + normalizedUserName);
        }

        SystemAdminEntity entity = new SystemAdminEntity();
        entity.setUsername(normalizedUserName);
        entity.setHashedPassword(passwordEncoder.encode(request.getPassword()));

        SystemAdminEntity saved = repository.save(entity);
        return new SystemAdminCreatedResponse(saved.getId(), saved.getUsername());
    }
}
