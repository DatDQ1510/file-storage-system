package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.tenantPlan.CreateTenantPlanRequest;
import com.java.file_storage_system.dto.tenantPlan.TenantPlanResponse;
import com.java.file_storage_system.dto.tenantPlan.UpdateTenantPlanRequest;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.TenantPlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/tenant-plans")
public class TenantPlanController {

    private final TenantPlanService tenantPlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantPlanResponse>>> getAllTenantPlans(HttpServletRequest httpServletRequest) {
        List<TenantPlanResponse> tenantPlans = tenantPlanService.getAllTenantPlans();
        return ResponseEntity.ok(ApiResponse.success("Get tenant plans successfully", tenantPlans, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{tenantPlanId}")
    public ResponseEntity<ApiResponse<TenantPlanResponse>> getTenantPlanById(
            @PathVariable("tenantPlanId") String tenantPlanId,
            HttpServletRequest httpServletRequest
    ) {
        TenantPlanResponse tenantPlan = tenantPlanService.getTenantPlanById(tenantPlanId);

        return ResponseEntity.ok(ApiResponse.success("Get tenant plan successfully", tenantPlan, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantPlanResponse>> createTenantPlan(
            @Valid @RequestBody CreateTenantPlanRequest request,
            HttpServletRequest httpServletRequest
    ) {
        TenantPlanResponse created = tenantPlanService.createTenantPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create tenant plan successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{tenantPlanId}")
    public ResponseEntity<ApiResponse<TenantPlanResponse>> updateTenantPlan(
            @PathVariable("tenantPlanId") String tenantPlanId,
            @Valid @RequestBody UpdateTenantPlanRequest request,
            HttpServletRequest httpServletRequest
    ) {
        TenantPlanResponse updated = tenantPlanService.updateTenantPlan(tenantPlanId, request);

        return ResponseEntity.ok(ApiResponse.success("Update tenant plan successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{tenantPlanId}")
    public ResponseEntity<ApiResponse<String>> deleteTenantPlan(
            @PathVariable("tenantPlanId") String tenantPlanId,
            HttpServletRequest httpServletRequest
    ) {
        tenantPlanService.deleteTenantPlan(tenantPlanId);
        return ResponseEntity.ok(ApiResponse.success("Delete tenant plan successfully", httpServletRequest.getRequestURI()));
    }
}
