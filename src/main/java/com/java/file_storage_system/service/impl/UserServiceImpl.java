package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.TenantPlanStatus;
import com.java.file_storage_system.constant.UserStatus;
import com.java.file_storage_system.dto.user.allUser.AllUserPageResponse;
import com.java.file_storage_system.dto.user.allUser.AllUserResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<UserEntity, UserRepository> implements UserService {

    private final TenantAdminRepository tenantAdminRepository;
    private final TenantPlanRepository tenantPlanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserCreatedResponse createUserByTenantAdmin(String tenantAdminId, CreateTenantUserRequest request) {
		log.info("Starting create user flow: tenantAdminId={}, rawUserName={}, rawEmail={}", tenantAdminId, request.getUserName(), request.getEmail());
        TenantAdminEntity tenantAdmin = findTenantAdminOrThrow(tenantAdminId);
        TenantEntity tenant = tenantAdmin.getTenant();
        log.debug("Resolved tenant for admin: tenantAdminId={}, tenantId={}", tenantAdminId, tenant.getId());

        validateTenantCapacity(tenant); // Check tenant capacity before validating duplicates to avoid unnecessary checks
        validateDuplicateUser(request, tenant.getId()); //  Check for duplicate email and username within the tenant

        UserEntity userToCreate = buildUserEntity(request, tenant); // Build the UserEntity from the request and tenant information
        UserEntity savedUser = repository.save(userToCreate); // Save the new user to the database
        log.info("User created successfully: userId={}, tenantId={}, userName={}", savedUser.getId(), tenant.getId(), savedUser.getUserName());
        return mapCreatedUser(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AllUserPageResponse getAllUsersInTenant(String tenantId, int page, int offset) {
        int normalizedPage = Math.max(page, 0);
        int normalizedOffset = Math.max(1, offset);

        log.info(
            "Loading users by tenant with pagination input tenantId={}, page={}, offset={}, normalizedPage={}, normalizedOffset={}",
            tenantId,
            page,
            offset,
            normalizedPage,
            normalizedOffset
        );

        Page<UserEntity> userPage = repository.findAllByTenantId(
            tenantId,
            PageRequest.of(normalizedPage, normalizedOffset)
        );

        List<AllUserResponse> items = userPage.getContent()
            .stream()
            .map(this::mapToAllUserResponse)
            .toList();

        log.info(
            "Loaded users by tenant successfully: tenantId={}, page={}, offset={}, totalElements={}, totalPages={}, returnedItems={}",
            tenantId,
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages(),
            items.size()
        );

        return new AllUserPageResponse(
            items,
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages(),
            userPage.hasNext(),
            userPage.hasPrevious()
        );
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

            if (normalizedKeyword == null) {
                Page<UserEntity> userPage = repository.findAllByTenantId(
                    tenantId,
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
		log.debug("Tenant capacity check: tenantId={}, currentUsers={}, maxUsers={}", tenant.getId(), currentUsers, maxUsers);

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
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedUserName = normalizeUserName(request.getUserName());

        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Duplicate email detected while creating user: tenantId={}, email={}", tenantId, normalizedEmail);
            throw new ConflictException("User email already exists: " + normalizedEmail);
        }

        if (repository.existsByUserNameIgnoreCaseAndTenantId(normalizedUserName, tenantId)) {
            log.warn("Duplicate username detected while creating user: tenantId={}, userName={}", tenantId, normalizedUserName);
            throw new ConflictException("Username already exists in tenant: " + normalizedUserName);
        }
    }

    private UserEntity buildUserEntity(CreateTenantUserRequest request, TenantEntity tenant) {
        UserEntity user = new UserEntity();
        user.setUserName(normalizeUserName(request.getUserName()));
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setTenant(tenant);
        user.setStatusUser(UserStatus.ACTIVE);
        user.setPhoneNumber(normalizeNullable(request.getPhoneNumber()));
        user.setDepartment(normalizeNullableLowerCase(request.getDepartment()));
        return user;
    }

    private UserCreatedResponse mapCreatedUser(UserEntity savedUser) {
        return new UserCreatedResponse(
                savedUser.getId(),
                savedUser.getUserName(),
                savedUser.getEmail(),
                savedUser.getTenant().getId(),
                savedUser.getPhoneNumber(),
                savedUser.getDepartment(),
                savedUser.getCreatedAt(),
            BigInteger.ZERO,
            Boolean.FALSE
        );
    }

    private AllUserResponse mapToAllUserResponse(UserEntity user) {
        boolean mfaEnabled = user.getSecretKey() != null && !user.getSecretKey().isBlank();

        return new AllUserResponse(
            user.getId(),
            user.getUserName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getDepartment(),
            user.getCreatedAt(),
            user.getStatusUser(),
            mfaEnabled,
            BigInteger.ZERO
        );
    }

    private String normalizeUserName(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeNullableLowerCase(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
