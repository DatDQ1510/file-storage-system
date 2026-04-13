package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.systemAdmin.create.CreateSystemAdminRequest;
import com.java.file_storage_system.dto.systemAdmin.create.SystemAdminCreatedResponse;
import com.java.file_storage_system.dto.systemAdmin.initial.CreateInitialTenantSetupRequest;
import com.java.file_storage_system.dto.systemAdmin.initial.InitialTenantSetupResponse;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.SystemAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/system-admins")
public class SystemAdminController {

    private static final String BOOTSTRAP_SECRET_HEADER = "X-System-Admin-Bootstrap-Secret";

    private final SystemAdminService systemAdminService;

    @PostMapping("/bootstrap")
    public ResponseEntity<ApiResponse<SystemAdminCreatedResponse>> bootstrapSystemAdmin(
            @RequestHeader(BOOTSTRAP_SECRET_HEADER) String bootstrapSecret,
            @Valid @RequestBody CreateSystemAdminRequest request,
            HttpServletRequest httpServletRequest
    ) {
        SystemAdminCreatedResponse created = systemAdminService.bootstrapSystemAdmin(bootstrapSecret, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "SystemAdmin bootstrap successfully",
                        created,
                        httpServletRequest.getRequestURI()
                ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemAdminCreatedResponse>> createSystemAdmin(
            @Valid @RequestBody CreateSystemAdminRequest request,
            HttpServletRequest httpServletRequest
    ) {
        SystemAdminCreatedResponse created = systemAdminService.createSystemAdminBySystemAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "SystemAdmin created successfully",
                        created,
                        httpServletRequest.getRequestURI()
                ));
    }

    @PostMapping("/create-initial")
    public ResponseEntity<ApiResponse<InitialTenantSetupResponse>> createInitialTenantSetup(
            @Valid @RequestBody CreateInitialTenantSetupRequest request,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Create initial tenant setup request: nameTenant={}, subdomain={}, username={}, email={}, phoneNumber={}, planId={}",
                request.nameTenant(),
                request.subdomain(),
                request.username(),
                request.email(),
                request.phoneNumber(),
                request.planId()
        );

        InitialTenantSetupResponse created = systemAdminService.createInitialTenantSetup(request);

        log.info("Initial tenant setup created successfully: tenantId={}, tenantAdminId={}, tenantPlanId={}",
                created.tenantId(),
                created.tenantAdminId(),
                created.tenantPlanId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Initial tenant setup created successfully",
                        created,
                        httpServletRequest.getRequestURI()
                ));
    }
}