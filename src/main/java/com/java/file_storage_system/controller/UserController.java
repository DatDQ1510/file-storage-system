package com.java.file_storage_system.controller;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.custom.RequireRole;
import com.java.file_storage_system.dto.user.allUser.AllUserPageResponse;
import com.java.file_storage_system.dto.user.createUser.CreateTenantUserRequest;
import com.java.file_storage_system.dto.user.createUser.UserCreatedResponse;
import com.java.file_storage_system.dto.user.changePassword.ResetUserPasswordByTenantAdminRequest;
import com.java.file_storage_system.dto.user.searchUser.UserSearchPageResponse;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.UnauthorizedException;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.UserService;
import com.java.file_storage_system.custom.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@AllArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@GetMapping
	@RequireRole(UserRole.TENANT_ADMIN)
	public ResponseEntity<ApiResponse<AllUserPageResponse>> getAllUsers(
			Authentication authentication,
			@RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
			@RequestParam(value = "offset", defaultValue = "10") @Min(1) int offset,
			HttpServletRequest httpServletRequest
	) {
		CustomUserDetails principal = extractPrincipal(authentication);
		String tenantId = principal.getTenantId();

		if (tenantId == null || tenantId.isBlank()) {
			throw new ForbiddenException("Tenant scope is required for listing users");
		}

		log.info(
			"GET /users called with tenantId={}, page={}, offset={}, path={}",
			tenantId,
			page,
			offset,
			httpServletRequest.getRequestURI()
		);

		AllUserPageResponse users = userService.getAllUsersInTenant(tenantId, page, offset);

		log.info(
			"GET /users success with tenantId={}, page={}, offset={}, totalElements={}, totalPages={}, returnedItems={}",
			tenantId,
			users.page(),
			users.offset(),
			users.totalElements(),
			users.totalPages(),
			users.items().size()
		);

		return ResponseEntity.ok(ApiResponse.success("Get users successfully", users, httpServletRequest.getRequestURI()));
	}


	@PostMapping
	@RequireRole(UserRole.TENANT_ADMIN)
	public ResponseEntity<ApiResponse<UserCreatedResponse>> registerUserByTenantAdmin(
			Authentication authentication,
			@Valid @RequestBody CreateTenantUserRequest request,
			HttpServletRequest httpServletRequest
	) {
		CustomUserDetails principal = extractPrincipal(authentication);
		String tenantAdminId = principal.getId();
		log.info(
			"Create user request received: tenantAdminId={}, tenantId={}, userName={}, email={}",
			tenantAdminId,
			principal.getTenantId(),
			request.getUserName(),
			request.getEmail()
		);
		UserCreatedResponse createdUser = userService.createUserByTenantAdmin(tenantAdminId, request);
		log.info(
			"Create user success: tenantAdminId={}, userId={}, userName={}, tenantId={}",
			tenantAdminId,
			createdUser.getId(),
			createdUser.getUserName(),
			createdUser.getTenantId()
		);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.success(
						"User registered successfully",
						createdUser,
						httpServletRequest.getRequestURI()
				));
	}

	@PatchMapping("reset-password")
	@RequireRole(UserRole.TENANT_ADMIN)
	public ResponseEntity<ApiResponse<String>> resetUserPasswordByTenantAdmin(
			Authentication authentication,
			@PathVariable("userId") String userId,
			@Valid @RequestBody ResetUserPasswordByTenantAdminRequest request,
			HttpServletRequest httpServletRequest
	) {
		CustomUserDetails principal = extractPrincipal(authentication);
		String tenantAdminId = principal.getId();

		userService.resetUserPasswordByTenantAdmin(tenantAdminId, userId, request);

		return ResponseEntity.ok(
				ApiResponse.success("Reset user password successfully", httpServletRequest.getRequestURI())
		);
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<UserSearchPageResponse>> searchUsersInTenant(
			Authentication authentication,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
			@RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) int size,
			HttpServletRequest httpServletRequest
	) {
		CustomUserDetails principal = extractPrincipal(authentication);
		String tenantId = principal.getTenantId();

		if (tenantId == null || tenantId.isBlank()) {
			throw new ForbiddenException("Tenant scope is required for user search");
		}

		UserSearchPageResponse users = userService.searchUsersInTenant(tenantId, keyword, page, size);

		return ResponseEntity.ok(
				ApiResponse.success("Search users successfully", users, httpServletRequest.getRequestURI())
		);
	}

	private CustomUserDetails extractPrincipal(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
			throw new UnauthorizedException("Invalid authentication principal");
		}
		return principal;
	}
}
