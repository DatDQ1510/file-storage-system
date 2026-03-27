package com.java.file_storage_system.controller;

import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.dto.project.ProjectPageResponse;
import com.java.file_storage_system.dto.project.ProjectRequest;
import com.java.file_storage_system.dto.project.ProjectResponse;
import com.java.file_storage_system.service.ProjectService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserContext userContext;

    /**
     * Tạo project mới - Chỉ TenantAdmin có thể tạo
     * 
     * @param request ProjectRequest chứa nameProject và ownerId
     * @return ProjectResponse
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        log.info("Received request to create project: {} by user: {}", request.nameProject(), userContext.getUsername());

        // Kiểm tra user có phải TenantAdmin không
        if (!userContext.isTenantAdmin()) {
            log.warn("User {} không phải TenantAdmin", userContext.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Tạo project
        ProjectResponse response = projectService.createProject(request, userContext.getId());
        log.info("Project created successfully with ID: {}", response.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ProjectPageResponse> getAllProjectsByTenantAdmin(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        if (!userContext.isTenantAdmin()) {
            log.warn("User {} không phải TenantAdmin", userContext.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProjectPageResponse response = projectService.getAllProjectsByTenantAdmin(userContext.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ProjectPageResponse> searchProjectsByTenantAdmin(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        if (!userContext.isTenantAdmin()) {
            log.warn("User {} không phải TenantAdmin", userContext.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProjectPageResponse response = projectService.searchProjectsByTenantAdmin(userContext.getId(), keyword, page, size);
        return ResponseEntity.ok(response);
    }
}
