package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.tenantAdmin.check.CheckTenantAdminRequest;
import com.java.file_storage_system.dto.tenantAdmin.check.CheckTenantAdminResponse;
import com.java.file_storage_system.dto.tenantAdmin.create.CreateTenantAdminRequest;
import com.java.file_storage_system.dto.tenantAdmin.create.TenantAdminCreatedResponse;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.service.TenantAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantAdminServiceImpl extends BaseServiceImpl<TenantAdminEntity, TenantAdminRepository> implements TenantAdminService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public TenantAdminCreatedResponse createTenantAdminBySystemAdmin(CreateTenantAdminRequest request) {
        TenantEntity tenant = findTenantOrThrow(request.getTenantId());
        validateDuplicate(request);

        TenantAdminEntity entity = new TenantAdminEntity();
        entity.setUserName(request.getUserName().trim().toLowerCase());
        entity.setEmail(request.getEmail().trim().toLowerCase());
        entity.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        entity.setPhoneNumber(request.getPhoneNumber().trim().toLowerCase());
        entity.setTenant(tenant);

        TenantAdminEntity saved = repository.save(entity);
        return new TenantAdminCreatedResponse(
                saved.getId(),
                saved.getTenant().getId(),
                saved.getUserName(),
                saved.getEmail()
        );
    }

    @Override
    public CheckTenantAdminResponse checkTenantAdmin(CheckTenantAdminRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizePhone(request.phoneNumber());

        boolean emailExists = repository.existsByEmailIgnoreCase(normalizedEmail);
        boolean sdtExists = repository.existsByPhoneNumber(normalizedPhone);
        boolean available = !emailExists && !sdtExists;

        return new CheckTenantAdminResponse(emailExists, sdtExists, available);
    }

    @Override
    public CheckTenantAdminResponse checkTenantAdmin(String username, String email, String sdt) {
        return checkTenantAdmin(new CheckTenantAdminRequest(username, email, sdt));
    }

    private TenantEntity findTenantOrThrow(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));
    }

    private void validateDuplicate(CreateTenantAdminRequest request) {
        String normalizedUserName = normalizeUserName(request.getUserName());
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (repository.existsByUserNameIgnoreCase(normalizedUserName)) {
            throw new ConflictException("Tenant admin userName already exists: " + normalizedUserName);
        }

        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Tenant admin email already exists: " + normalizedEmail);
        }
    }

    private String normalizeUserName(String userName) {
        return userName == null ? null : userName.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.trim();
    }
}
