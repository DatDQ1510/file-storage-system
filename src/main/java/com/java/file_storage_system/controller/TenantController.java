package com.java.file_storage_system.controller;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.custom.RequireRole;
import com.java.file_storage_system.dto.tenant.CreateTenantRequest;
import com.java.file_storage_system.dto.tenant.TenantResponse;
import com.java.file_storage_system.dto.tenant.UpdateTenantRequest;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
	@RequireRole(UserRole.SYSTEM_ADMIN)
	public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllTenants(HttpServletRequest httpServletRequest) {
	List<TenantResponse> tenants = tenantService.findAll().stream().map(this::mapToResponse).toList();
	return ResponseEntity.ok(ApiResponse.success("Get tenants successfully", tenants, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(
	    @PathVariable("tenantId") String tenantId,
	    HttpServletRequest httpServletRequest
    ) {
		TenantEntity tenant = tenantService.findById(tenantId)
			.orElseThrow(() -> ResourceNotFoundException.byField("Tenant", "id", tenantId));

		return ResponseEntity.ok(
			ApiResponse.success("Get tenant successfully", mapToResponse(tenant), httpServletRequest.getRequestURI())
		);
    }

    @PostMapping
	@RequireRole(UserRole.SYSTEM_ADMIN)
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
	    @Valid @RequestBody CreateTenantRequest request,
	    HttpServletRequest httpServletRequest
    ) {
		TenantEntity created = tenantService.createTenant(request);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success("Create tenant successfully", mapToResponse(created), httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{tenantId}")
	@RequireRole(UserRole.SYSTEM_ADMIN)
	public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
	    @PathVariable("tenantId") String tenantId,
	    @Valid @RequestBody UpdateTenantRequest request,
	    HttpServletRequest httpServletRequest
    ) {
		TenantEntity updated = tenantService.updateTenant(tenantId, request);

		return ResponseEntity.ok(
			ApiResponse.success("Update tenant successfully", mapToResponse(updated), httpServletRequest.getRequestURI())
		);
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<String>> deleteTenant(
	    @PathVariable("tenantId") String tenantId,
	    HttpServletRequest httpServletRequest
    ) {
		tenantService.findById(tenantId)
			.orElseThrow(() -> ResourceNotFoundException.byField("Tenant", "id", tenantId));

		tenantService.deleteById(tenantId);
		return ResponseEntity.ok(ApiResponse.success("Delete tenant successfully", httpServletRequest.getRequestURI()));
    }

	@GetMapping("/check-tenant")
	public ResponseEntity<ApiResponse<Boolean>> checkTenant(
	    @RequestParam("domainTenant") @NotBlank(message = "domainTenant is required") String domainTenant,
	    HttpServletRequest httpServletRequest
	) {
		boolean exists = tenantService.existsByDomainTenant(domainTenant);
		String message = exists
			? "Domain tenant already exists"
			: "Domain tenant is available";

		return ResponseEntity.ok(ApiResponse.success(message, exists, httpServletRequest.getRequestURI()));
	}

    private TenantResponse mapToResponse(TenantEntity tenant) {
		return new TenantResponse(
			tenant.getId(),
			tenant.getNameTenant(),
			tenant.getDomainTenant(),
			tenant.getExTraStorageSize(),
			tenant.getUsedStorageSize(),
			tenant.getStatusTenant(),
			tenant.getCreatedAt(),
			tenant.getUpdatedAt()
		);
    }
}
