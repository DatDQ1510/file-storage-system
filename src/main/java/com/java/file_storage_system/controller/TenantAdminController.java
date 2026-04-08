package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.tenantAdmin.check.CheckTenantAdminResponse;
import com.java.file_storage_system.dto.tenantAdmin.create.CreateTenantAdminRequest;
import com.java.file_storage_system.dto.tenantAdmin.create.TenantAdminCreatedResponse;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.TenantAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
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

    @GetMapping("/check-tenantadmin")
    public ResponseEntity<ApiResponse<CheckTenantAdminResponse>> checkTenantAdmin(
                @RequestParam("username") @NotBlank(message = "username is required") String username,
                @RequestParam("email") @NotBlank(message = "email is required") String email,
                @RequestParam("sdt") @NotBlank(message = "sdt is required") String sdt,
        HttpServletRequest httpServletRequest
    ) {
                CheckTenantAdminResponse checkResult = tenantAdminService.checkTenantAdmin(username, email, sdt);
        String message = checkResult.available()
                ? "Email and sdt are available"
                : "Email or sdt already exists";

        return ResponseEntity.ok(
                ApiResponse.success(message, checkResult, httpServletRequest.getRequestURI())
        );
    }
}