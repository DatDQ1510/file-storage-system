package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.user.createUser.CreateTenantUserRequest;
import com.java.file_storage_system.dto.user.createUser.UserCreatedResponse;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.TenantPlan;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.repository.TenantPlanRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<UserEntity, UserRepository> implements UserService {

    private final TenantAdminRepository tenantAdminRepository;
    private final TenantPlanRepository tenantPlanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserCreatedResponse createUserByTenantAdmin(String tenantAdminId, CreateTenantUserRequest request) {
        TenantAdminEntity tenantAdmin = findTenantAdminOrThrow(tenantAdminId);
        TenantEntity tenant = tenantAdmin.getTenant();

        validateTenantCapacity(tenant); // Check tenant capacity before validating duplicates to avoid unnecessary checks
        validateDuplicateUser(request, tenant.getId()); //  Check for duplicate email and username within the tenant

        UserEntity userToCreate = buildUserEntity(request, tenant); // Build the UserEntity from the request and tenant information
        UserEntity savedUser = repository.save(userToCreate); // Save the new user to the database
        return mapCreatedUser(savedUser);
    }

    private TenantAdminEntity findTenantAdminOrThrow(String tenantAdminId) {
        return tenantAdminRepository.findById(tenantAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant admin not found with id: " + tenantAdminId));
    }

    private void validateTenantCapacity(TenantEntity tenant) {
        TenantPlan tenantPlan = findTenantPlanOrThrow(tenant.getId());
        SubscriptionPlanEntity subscriptionPlan = tenantPlan.getPlan();

        if (subscriptionPlan == null || subscriptionPlan.getMaxUsers() == null) {
            throw new ResourceNotFoundException("Active subscription plan is missing or maxUsers is not configured for tenant: " + tenant.getId());
        }

        long currentUsers = repository.countByTenantId(tenant.getId());
        int maxUsers = subscriptionPlan.getMaxUsers();

        if (currentUsers >= maxUsers) {
            throw new ConflictException("Tenant has reached user limit. Current users: " + currentUsers + ", max users: " + maxUsers);
        }
    }

    private TenantPlan findTenantPlanOrThrow(String tenantId) {
        return tenantPlanRepository.findLatestByTenantIdAndStatus(tenantId, "ACTIVE")
            .or(() -> tenantPlanRepository.findLatestByTenantId(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Tenant plan not found for tenant: " + tenantId));
    }

    private void validateDuplicateUser(CreateTenantUserRequest request, String tenantId) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User email already exists: " + request.getEmail());
        }

        if (repository.existsByUserNameAndTenantId(request.getUserName(), tenantId)) {
            throw new ConflictException("Username already exists in tenant: " + request.getUserName());
        }
    }

    private UserEntity buildUserEntity(CreateTenantUserRequest request, TenantEntity tenant) {
        UserEntity user = new UserEntity();
        user.setUserName(request.getUserName().trim().toLowerCase());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setTenant(tenant);
        return user;
    }

    private UserCreatedResponse mapCreatedUser(UserEntity savedUser) {
        return new UserCreatedResponse(
                savedUser.getId(),
                savedUser.getUserName(),
                savedUser.getEmail(),
                savedUser.getTenant().getId()
        );
    }
}
