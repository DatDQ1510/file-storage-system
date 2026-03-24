package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.systemAdmin.create.CreateSystemAdminRequest;
import com.java.file_storage_system.dto.systemAdmin.create.SystemAdminCreatedResponse;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.SystemAdminService;
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
}