package com.java.file_storage_system.controller;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.custom.RequireFolderPermission;
import com.java.file_storage_system.custom.RequirePermission;
import com.java.file_storage_system.custom.RequireRole;
import com.java.file_storage_system.dto.folder.CreateFolderWithAclRequest;
import com.java.file_storage_system.dto.folder.CreateFolderWithAclResponse;
import com.java.file_storage_system.dto.folder.FolderPathNodeResponse;
import com.java.file_storage_system.dto.folder.FolderResponse;
import com.java.file_storage_system.dto.folder.ProjectMemberForAclResponse;
import com.java.file_storage_system.dto.project.ProjectPageResponse;
import com.java.file_storage_system.dto.project.ProjectRequest;
import com.java.file_storage_system.dto.project.ProjectResponse;
import com.java.file_storage_system.dto.project.UpdateProjectRequest;
import com.java.file_storage_system.dto.project.member.AssignProjectMemberRequest;
import com.java.file_storage_system.dto.project.member.ProjectMemberResponse;
import com.java.file_storage_system.dto.project.member.UpdateProjectMemberPermissionRequest;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.FolderService;
import com.java.file_storage_system.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final FolderService folderService;
    private final UserContext userContext;

    @PostMapping
    @RequireRole(UserRole.TENANT_ADMIN)
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectRequest request,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Received request to create project: {} by user: {}", request.nameProject(), userContext.getUsername());

        requireTenantAdmin();

        // Tạo project
        ProjectResponse response = projectService.createProject(request, userContext.getId());
        log.info("Project created successfully with ID: {}", response.id());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Create project successfully", response, httpServletRequest.getRequestURI()));
    }

    @GetMapping
    @RequireRole(UserRole.TENANT_ADMIN)
    public ResponseEntity<ApiResponse<ProjectPageResponse>> searchProjectsByTenantAdmin(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) int size,
        HttpServletRequest httpServletRequest
    ) {
        requireTenantAdmin();

        ProjectPageResponse response = projectService.searchProjectsByTenantAdmin(userContext.getId(), keyword, page, size);
        return ResponseEntity.ok(
            ApiResponse.success("Search projects successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @GetMapping("/my-projects")
    @RequireRole(UserRole.USER)
    public ResponseEntity<ApiResponse<ProjectPageResponse>> getAllProjectsByUser(
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) int size,
            HttpServletRequest httpServletRequest
    ) {
        ProjectPageResponse response = projectService.getAllProjectsByUser(userContext.getId(), page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Get user projects successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @PostMapping("/{projectId}/members/assign")
    @RequireRole(UserRole.USER)
    @RequirePermission(RequirePermission.Permission.MANAGE_MEMBER)
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> assignMemberToProject(
        @PathVariable("projectId") String projectId,
        @Valid @RequestBody AssignProjectMemberRequest request,
        HttpServletRequest httpServletRequest
    ) {
        ProjectMemberResponse response = projectService.assignMemberToProject(
            projectId,
            request,
            userContext.getId(),
            userContext.getRole(),
            userContext.getTenantId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Assign project member successfully", response, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{projectId}/members")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable("projectId") String projectId,
            HttpServletRequest httpServletRequest
    ) {
        List<ProjectMemberResponse> response = projectService.getProjectMembers(
                projectId,
                userContext.getId(),
            userContext.getRole(),
                userContext.getTenantId()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Get project members successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @PatchMapping("/{projectId}/members/{memberUserId}/permission")
    @RequireRole(UserRole.USER)
    @RequirePermission(RequirePermission.Permission.MANAGE_MEMBER)
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> updateProjectMemberPermission(
            @PathVariable("projectId") String projectId,
            @PathVariable("memberUserId") String memberUserId,
            @Valid @RequestBody UpdateProjectMemberPermissionRequest request,
            HttpServletRequest httpServletRequest
    ) {
        ProjectMemberResponse response = projectService.updateProjectMemberPermission(
                projectId,
                memberUserId,
                request,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Update project member permission successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @DeleteMapping("/{projectId}/members/{memberUserId}")
    @RequireRole(UserRole.USER)
    @RequirePermission(RequirePermission.Permission.MANAGE_MEMBER)
    public ResponseEntity<ApiResponse<String>> removeProjectMember(
            @PathVariable("projectId") String projectId,
            @PathVariable("memberUserId") String memberUserId,
            HttpServletRequest httpServletRequest
    ) {
        projectService.removeProjectMember(
                projectId,
                memberUserId,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Remove project member successfully", httpServletRequest.getRequestURI())
        );
    }

    @PatchMapping("/{projectId}")
    @RequireRole(UserRole.TENANT_ADMIN)
//    @RequirePermission(RequirePermission.Permission.WRITE)
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable("projectId") String projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            HttpServletRequest httpServletRequest
    ) {
        ProjectResponse response = projectService.updateProject(
            projectId,
            request,
            userContext.getId(),
            userContext.getRole(),
            userContext.getTenantId()
        );

        return ResponseEntity.ok(
            ApiResponse.success("Update project successfully", response, httpServletRequest.getRequestURI())
        );
    }

    @DeleteMapping("/{projectId}")
    @RequireRole(UserRole.TENANT_ADMIN)
//    @RequirePermission(RequirePermission.Permission.DELETE)
    public ResponseEntity<ApiResponse<String>> deleteProject(
            @PathVariable("projectId") String projectId,
            HttpServletRequest httpServletRequest
    ) {
        projectService.deleteProject(
            projectId,
            userContext.getId(),
            userContext.getRole(),
            userContext.getTenantId()
        );

        return ResponseEntity.ok(
            ApiResponse.success("Delete project successfully", httpServletRequest.getRequestURI())
        );
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable("projectId") String projectId,
            HttpServletRequest httpServletRequest
    ) {
        ProjectResponse response = projectService.getProjectById(
                projectId,
                userContext.getId(),
            userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.ok(ApiResponse.success("Get project successfully", response, httpServletRequest.getRequestURI()));
    }

    private void requireTenantAdmin() {
        if (!userContext.isTenantAdmin()) {
            log.warn("User {} is not tenant admin", userContext.getUsername());
            throw new ForbiddenException("Only tenant admin can perform this action");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Folder endpoints (resource hierarchy: /projects/{projectId}/folders/*)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/v1/projects/{projectId}/folders
     * Tạo folder mới trong project, kèm ACL tuỳ chọn.
     * Body có thể chứa parentFolderId để tạo folder con.
     * Path được tự động tính: parentPath + "/" + folderName.
     */
    @PostMapping("/{projectId}/folders")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    public ResponseEntity<ApiResponse<CreateFolderWithAclResponse>> createFolder(
            @PathVariable("projectId") String projectId,
            @Valid @RequestBody CreateFolderWithAclRequest request,
            HttpServletRequest httpServletRequest
    ) {
        CreateFolderWithAclResponse response = folderService.createFolderWithAcl(
                projectId,
                request,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create folder successfully", response, httpServletRequest.getRequestURI()));
    }

    /**
     * GET /api/v1/projects/{projectId}/folders
     * Lấy tất cả folders của project (flat list).
     */
    @GetMapping("/{projectId}/folders")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
//    @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getProjectFolders(
            @PathVariable("projectId") String projectId,
            HttpServletRequest httpServletRequest
    ) {
        List<FolderResponse> folders = folderService.getFoldersByProject(
                projectId,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Get project folders successfully", folders, httpServletRequest.getRequestURI())
        );
    }

    /**
     * GET /api/v1/projects/{projectId}/folders/children?parentPath=/
     * Lấy các folder con trực tiếp theo đường dẫn cha (path-based tree traversal).
     * parentPath mặc định là "/" (root).
     */
    @GetMapping("/{projectId}/folders/children")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
//    @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
    public ResponseEntity<ApiResponse<List<FolderPathNodeResponse>>> getChildFolderPaths(
            @PathVariable("projectId") String projectId,
            @RequestParam(value = "parentPath", defaultValue = "/") String parentPath,
            HttpServletRequest httpServletRequest
    ) {
        List<FolderPathNodeResponse> response = folderService.getChildFolderPaths(
                projectId,
                parentPath,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Get child folder paths successfully", response, httpServletRequest.getRequestURI())
        );
    }

    /**
     * GET /api/v1/projects/{projectId}/folders/search?keyword=
     * Tìm kiếm folder theo tên/path trong project.
     */
    @GetMapping("/{projectId}/folders/search")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
//    @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
    public ResponseEntity<ApiResponse<List<FolderPathNodeResponse>>> searchFolders(
            @PathVariable("projectId") String projectId,
            @RequestParam("keyword") String keyword,
            HttpServletRequest httpServletRequest
    ) {
        List<FolderPathNodeResponse> response = folderService.searchFolderPaths(
                projectId,
                keyword,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Search folders successfully", response, httpServletRequest.getRequestURI())
        );
    }

    /**
     * GET /api/v1/projects/{projectId}/members-for-acl
     * Lấy danh sách members của project để dùng khi gán ACL cho folder.
     * Trả về owner + tất cả members, deduplicated.
     */
    @GetMapping("/{projectId}/members-for-acl")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    public ResponseEntity<ApiResponse<List<ProjectMemberForAclResponse>>> getProjectMembersForAcl(
            @PathVariable("projectId") String projectId,
            HttpServletRequest httpServletRequest
    ) {
        List<ProjectMemberForAclResponse> response = folderService.getProjectMembersForAcl(
                projectId,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Get project members for ACL successfully", response, httpServletRequest.getRequestURI())
        );
    }
}
