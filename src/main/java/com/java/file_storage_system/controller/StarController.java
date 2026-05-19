package com.java.file_storage_system.controller;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.dto.starred.StarredPageResponse;
import com.java.file_storage_system.service.StarService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/starred")
public class StarController {

    private final StarService starService;
    private final UserContext userContext;

    @GetMapping
    @com.java.file_storage_system.custom.RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    public ResponseEntity<ApiResponse<StarredPageResponse>> getStarredPage(
            HttpServletRequest httpServletRequest
    ) {
        StarredPageResponse response = starService.getStarredPage(userContext.getId(), userContext.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Get starred items successfully", response, httpServletRequest.getRequestURI()));
    }
}