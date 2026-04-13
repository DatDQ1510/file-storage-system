package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.TenantPlanStatus;
import com.java.file_storage_system.constant.TenantStatus;
import com.java.file_storage_system.dto.systemAdmin.create.CreateSystemAdminRequest;
import com.java.file_storage_system.dto.systemAdmin.create.SystemAdminCreatedResponse;
import com.java.file_storage_system.dto.systemAdmin.initial.CreateInitialTenantSetupRequest;
import com.java.file_storage_system.dto.systemAdmin.initial.InitialTenantSetupResponse;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import com.java.file_storage_system.entity.SystemAdminEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.TenantPlan;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.SubscriptionPlanRepository;
import com.java.file_storage_system.repository.SystemAdminRepository;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.repository.TenantPlanRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.service.SystemAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SystemAdminServiceImpl extends BaseServiceImpl<SystemAdminEntity, SystemAdminRepository> implements SystemAdminService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%!";
    private static final int GENERATED_PASSWORD_LENGTH = 14;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final TenantAdminRepository tenantAdminRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TenantPlanRepository tenantPlanRepository;

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

    @Override
    @Transactional
    public InitialTenantSetupResponse createInitialTenantSetup(CreateInitialTenantSetupRequest request) {
        String normalizedTenantName = normalize(request.nameTenant());
        String normalizedSubdomain = normalizeToLower(request.subdomain());
        String normalizedUserName = normalizeToLower(request.username());
        String normalizedEmail = normalizeToLower(request.email());
        String normalizedPhone = normalize(request.phoneNumber());

        if (tenantRepository.existsByDomainTenant(normalizedSubdomain)) {
            throw ConflictException.alreadyExists("Tenant", "domainTenant", normalizedSubdomain);
        }

        if (tenantAdminRepository.existsByUserNameIgnoreCase(normalizedUserName)) {
            throw new ConflictException("Tenant admin userName already exists: " + normalizedUserName);
        }

        if (tenantAdminRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Tenant admin email already exists: " + normalizedEmail);
        }

        if (tenantAdminRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new ConflictException("Tenant admin phone number already exists: " + normalizedPhone);
        }

        SubscriptionPlanEntity plan = subscriptionPlanRepository.findById(request.planId())
                .orElseThrow(() -> ResourceNotFoundException.byField("SubscriptionPlan", "id", request.planId()));

        TenantEntity tenant = new TenantEntity();
        tenant.setNameTenant(normalizedTenantName);
        tenant.setDomainTenant(normalizedSubdomain);
        tenant.setExTraStorageSize(BigInteger.ZERO);
        tenant.setUsedStorageSize(BigInteger.ZERO);
        tenant.setStatusTenant(TenantStatus.ACTIVE);
        TenantEntity savedTenant = tenantRepository.save(tenant);

        String generatedPassword = generatePassword();

        TenantAdminEntity tenantAdmin = new TenantAdminEntity();
        tenantAdmin.setUserName(normalizedUserName);
        tenantAdmin.setEmail(normalizedEmail);
        tenantAdmin.setPhoneNumber(normalizedPhone);
        tenantAdmin.setHashedPassword(passwordEncoder.encode(generatedPassword));
        tenantAdmin.setTenant(savedTenant);
        TenantAdminEntity savedTenantAdmin = tenantAdminRepository.save(tenantAdmin);

        LocalDateTime planStartDate = LocalDateTime.now();
        LocalDateTime planEndDate = switch (plan.getBillingCycle()) {
            case MONTHLY -> planStartDate.plusMonths(1);
            case QUARTERLY -> planStartDate.plusMonths(3);
            case YEARLY -> planStartDate.plusYears(1);
        };

        TenantPlan tenantPlan = new TenantPlan();
        tenantPlan.setStatus(TenantPlanStatus.ACTIVE);
        tenantPlan.setPlanStartDate(planStartDate);
        tenantPlan.setPlanEndDate(planEndDate);
        tenantPlan.setIsAutoRenew(Boolean.TRUE);
        tenantPlan.setTenant(savedTenant);
        tenantPlan.setPlan(plan);
        TenantPlan savedTenantPlan = tenantPlanRepository.save(tenantPlan);

        return new InitialTenantSetupResponse(
                savedTenant.getId(),
                savedTenant.getDomainTenant(),
                savedTenantAdmin.getId(),
                savedTenantAdmin.getUserName(),
                savedTenantPlan.getId(),
                plan.getId(),
                savedTenantPlan.getPlanStartDate(),
                savedTenantPlan.getPlanEndDate(),
                generatedPassword
        );
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
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (repository.existsByUserNameIgnoreCase(normalizedUserName)) {
            throw new ConflictException("System admin username already exists: " + normalizedUserName);
        }

        if (repository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ConflictException("System admin email already exists: " + normalizedEmail);
        }

        SystemAdminEntity entity = new SystemAdminEntity();
        entity.setUserName(normalizedUserName);
        entity.setEmail(normalizedEmail);
        entity.setHashedPassword(passwordEncoder.encode(request.getPassword()));

        SystemAdminEntity saved = repository.save(entity);
        return new SystemAdminCreatedResponse(saved.getId(), saved.getUserName());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeToLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }
}
