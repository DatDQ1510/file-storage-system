package com.java.file_storage_system.controller;

import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.SubscriptionPlanService;
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
@RequestMapping("/api/v1/subscription-plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPlanEntity>>> getAllSubscriptionPlans(HttpServletRequest httpServletRequest) {
        List<SubscriptionPlanEntity> plans = subscriptionPlanService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Get subscription plans successfully", plans, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{subscriptionPlanId}")
    public ResponseEntity<ApiResponse<SubscriptionPlanEntity>> getSubscriptionPlanById(
            @PathVariable("subscriptionPlanId") String subscriptionPlanId,
            HttpServletRequest httpServletRequest
    ) {
        SubscriptionPlanEntity plan = subscriptionPlanService.findById(subscriptionPlanId)
                .orElseThrow(() -> ResourceNotFoundException.byField("SubscriptionPlan", "id", subscriptionPlanId));

        return ResponseEntity.ok(ApiResponse.success("Get subscription plan successfully", plan, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanEntity>> createSubscriptionPlan(
            @Valid @RequestBody SubscriptionPlanEntity request,
            HttpServletRequest httpServletRequest
    ) {
        // Prevent overwrite-by-id when creating new records.
        request.setId(null);
        SubscriptionPlanEntity created = subscriptionPlanService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create subscription plan successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{subscriptionPlanId}")
    public ResponseEntity<ApiResponse<SubscriptionPlanEntity>> updateSubscriptionPlan(
            @PathVariable("subscriptionPlanId") String subscriptionPlanId,
            @Valid @RequestBody SubscriptionPlanEntity request,
            HttpServletRequest httpServletRequest
    ) {
        subscriptionPlanService.findById(subscriptionPlanId)
                .orElseThrow(() -> ResourceNotFoundException.byField("SubscriptionPlan", "id", subscriptionPlanId));

        request.setId(subscriptionPlanId);
        SubscriptionPlanEntity updated = subscriptionPlanService.save(request);

        return ResponseEntity.ok(ApiResponse.success("Update subscription plan successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{subscriptionPlanId}")
    public ResponseEntity<ApiResponse<String>> deleteSubscriptionPlan(
            @PathVariable("subscriptionPlanId") String subscriptionPlanId,
            HttpServletRequest httpServletRequest
    ) {
        subscriptionPlanService.findById(subscriptionPlanId)
                .orElseThrow(() -> ResourceNotFoundException.byField("SubscriptionPlan", "id", subscriptionPlanId));

        subscriptionPlanService.deleteById(subscriptionPlanId);
        return ResponseEntity.ok(ApiResponse.success("Delete subscription plan successfully", httpServletRequest.getRequestURI()));
    }
}
