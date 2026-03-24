package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.tenantAdmin.create.CreateTenantAdminRequest;
import com.java.file_storage_system.dto.tenantAdmin.create.TenantAdminCreatedResponse;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.TenantAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/tenant-admins")
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantAdminCreatedResponse>> createTenantAdmin(
            @Valid @RequestBody CreateTenantAdminRequest request,
            HttpServletRequest httpServletRequest
    ) {
        TenantAdminCreatedResponse created = tenantAdminService.createTenantAdminBySystemAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "TenantAdmin created successfully",
                        created,
                        httpServletRequest.getRequestURI()
                ));
    }
}