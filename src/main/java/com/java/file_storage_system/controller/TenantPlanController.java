package com.java.file_storage_system.controller;

import com.java.file_storage_system.entity.TenantPlan;
import com.java.file_storage_system.exception.ResourceNotFoundException;
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
    public ResponseEntity<ApiResponse<List<TenantPlan>>> getAllTenantPlans(HttpServletRequest httpServletRequest) {
        List<TenantPlan> tenantPlans = tenantPlanService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Get tenant plans successfully", tenantPlans, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{tenantPlanId}")
    public ResponseEntity<ApiResponse<TenantPlan>> getTenantPlanById(
            @PathVariable("tenantPlanId") String tenantPlanId,
            HttpServletRequest httpServletRequest
    ) {
        TenantPlan tenantPlan = tenantPlanService.findById(tenantPlanId)
                .orElseThrow(() -> ResourceNotFoundException.byField("TenantPlan", "id", tenantPlanId));

        return ResponseEntity.ok(ApiResponse.success("Get tenant plan successfully", tenantPlan, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantPlan>> createTenantPlan(
            @Valid @RequestBody TenantPlan request,
            HttpServletRequest httpServletRequest
    ) {
        // Prevent overwrite-by-id when creating new records.
        request.setId(null);
        TenantPlan created = tenantPlanService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create tenant plan successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{tenantPlanId}")
    public ResponseEntity<ApiResponse<TenantPlan>> updateTenantPlan(
            @PathVariable("tenantPlanId") String tenantPlanId,
            @Valid @RequestBody TenantPlan request,
            HttpServletRequest httpServletRequest
    ) {
        tenantPlanService.findById(tenantPlanId)
                .orElseThrow(() -> ResourceNotFoundException.byField("TenantPlan", "id", tenantPlanId));

        request.setId(tenantPlanId);
        TenantPlan updated = tenantPlanService.save(request);

        return ResponseEntity.ok(ApiResponse.success("Update tenant plan successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{tenantPlanId}")
    public ResponseEntity<ApiResponse<String>> deleteTenantPlan(
            @PathVariable("tenantPlanId") String tenantPlanId,
            HttpServletRequest httpServletRequest
    ) {
        tenantPlanService.findById(tenantPlanId)
                .orElseThrow(() -> ResourceNotFoundException.byField("TenantPlan", "id", tenantPlanId));

        tenantPlanService.deleteById(tenantPlanId);
        return ResponseEntity.ok(ApiResponse.success("Delete tenant plan successfully", httpServletRequest.getRequestURI()));
    }
}
