package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.user.createUser.CreateTenantUserRequest;
import com.java.file_storage_system.dto.user.createUser.UserCreatedResponse;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.security.TenantAdminApiInterceptor;
import com.java.file_storage_system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;


	@PostMapping("/tenant-admin/register")
	public ResponseEntity<ApiResponse<UserCreatedResponse>> registerUserByTenantAdmin(
			@RequestHeader(TenantAdminApiInterceptor.TENANT_ADMIN_HEADER) String tenantAdminId,
			@Valid @RequestBody CreateTenantUserRequest request,
			HttpServletRequest httpServletRequest
	) {
		UserCreatedResponse createdUser = userService.createUserByTenantAdmin(tenantAdminId, request);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(ApiResponse.success(
						"User registered successfully",
						createdUser,
						httpServletRequest.getRequestURI()
				));
	}
}
