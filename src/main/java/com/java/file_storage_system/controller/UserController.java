package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.user.createUser.CreateTenantUserRequest;
import com.java.file_storage_system.dto.user.createUser.UserCreatedResponse;
import com.java.file_storage_system.dto.user.changePassword.ResetUserPasswordByTenantAdminRequest;
import com.java.file_storage_system.dto.user.searchUser.UserSearchPageResponse;
import com.java.file_storage_system.exception.UnauthorizedException;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.UserService;
import com.java.file_storage_system.custom.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;


	@PostMapping("/tenant-admin/register")
	public ResponseEntity<ApiResponse<UserCreatedResponse>> registerUserByTenantAdmin(
			Authentication authentication,
			@Valid @RequestBody CreateTenantUserRequest request,
			HttpServletRequest httpServletRequest
	) {
		CustomUserDetails principal = extractPrincipal(authentication);
		String tenantAdminId = principal.getId();
		UserCreatedResponse createdUser = userService.createUserByTenantAdmin(tenantAdminId, request);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.success(
						"User registered successfully",
						createdUser,
						httpServletRequest.getRequestURI()
				));
	}

	@PatchMapping("/tenant-admin/{userId}/password")
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
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			HttpServletRequest httpServletRequest
	) {
		CustomUserDetails principal = extractPrincipal(authentication);
		String tenantId = principal.getTenantId();

		if (tenantId == null || tenantId.isBlank()) {
			return ResponseEntity
					.status(HttpStatus.FORBIDDEN)
					.body(ApiResponse.error("Tenant scope is required for user search", httpServletRequest.getRequestURI()));
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
