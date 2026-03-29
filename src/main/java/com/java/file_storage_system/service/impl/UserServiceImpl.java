package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.TenantPlanStatus;
import com.java.file_storage_system.dto.user.createUser.CreateTenantUserRequest;
import com.java.file_storage_system.dto.user.createUser.UserCreatedResponse;
import com.java.file_storage_system.dto.user.changePassword.ResetUserPasswordByTenantAdminRequest;
import com.java.file_storage_system.dto.user.searchUser.UserSearchItemResponse;
import com.java.file_storage_system.dto.user.searchUser.UserSearchPageResponse;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.TenantPlan;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.repository.TenantPlanRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Override
    @Transactional
    public void resetUserPasswordByTenantAdmin(String tenantAdminId, String userId, ResetUserPasswordByTenantAdminRequest request) {
        TenantAdminEntity tenantAdmin = findTenantAdminOrThrow(tenantAdminId);
        UserEntity user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!tenantAdmin.getTenant().getId().equals(user.getTenant().getId())) {
            throw new ForbiddenException("Tenant admin cannot reset password of user outside tenant");
        }

        user.setHashedPassword(passwordEncoder.encode(request.newPassword()));
        repository.save(user);
    }

        @Override
        @Transactional(readOnly = true)
        public UserSearchPageResponse searchUsersInTenant(String tenantId, String keyword, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        String normalizedKeyword = normalizeKeyword(keyword);

        Page<UserEntity> userPage = repository.searchByTenantIdAndKeyword(
            tenantId,
            normalizedKeyword,
            PageRequest.of(normalizedPage, normalizedSize)
        );

        List<UserSearchItemResponse> items = userPage.getContent()
            .stream()
            .map(user -> new UserSearchItemResponse(user.getId(), user.getUserName(), user.getEmail()))
            .toList();

        return new UserSearchPageResponse(
            items,
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages(),
            userPage.hasNext(),
            userPage.hasPrevious()
        );
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
        return tenantPlanRepository.findLatestByTenantIdAndStatus(tenantId, TenantPlanStatus.ACTIVE)
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
