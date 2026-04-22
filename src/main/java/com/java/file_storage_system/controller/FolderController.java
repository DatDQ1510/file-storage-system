package com.java.file_storage_system.controller;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.custom.RequireFolderPermission;
import com.java.file_storage_system.custom.RequireRole;
import com.java.file_storage_system.dto.folder.CreateFolderRequest;
import com.java.file_storage_system.dto.folder.CreateFolderWithAclRequest;
import com.java.file_storage_system.dto.folder.CreateFolderWithAclResponse;
import com.java.file_storage_system.dto.folder.FolderAclItemResponse;
import com.java.file_storage_system.dto.folder.FolderPathNodeResponse;
import com.java.file_storage_system.dto.folder.FolderResponse;
import com.java.file_storage_system.dto.folder.ProjectMemberForAclResponse;
import com.java.file_storage_system.dto.folder.RenameFolderRequest;
import com.java.file_storage_system.dto.folder.UpdateFolderRequest;
import com.java.file_storage_system.dto.folder.UpsertFolderAclRequest;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.FolderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/folders")
public class FolderController {

    private final FolderService folderService;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getAllFolders(HttpServletRequest httpServletRequest) {
	List<FolderResponse> folders = folderService.getAllFolders();
	return ResponseEntity.ok(ApiResponse.success("Get folders successfully", folders, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<ApiResponse<FolderResponse>> getFolderById(
	    @PathVariable("folderId") String folderId,
	    HttpServletRequest httpServletRequest
    ) {
	FolderResponse folder = folderService.getFolderById(folderId);

	return ResponseEntity.ok(
		ApiResponse.success("Get folder successfully", folder, httpServletRequest.getRequestURI())
	);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
	    @Valid @RequestBody CreateFolderRequest request,
	    HttpServletRequest httpServletRequest
    ) {
	FolderResponse created = folderService.createFolder(request);

	return ResponseEntity.status(HttpStatus.CREATED)
		.body(ApiResponse.success("Create folder successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{folderId}")
    public ResponseEntity<ApiResponse<FolderResponse>> updateFolder(
	    @PathVariable("folderId") String folderId,
	    @Valid @RequestBody UpdateFolderRequest request,
	    HttpServletRequest httpServletRequest
    ) {
	FolderResponse updated = folderService.updateFolder(folderId, request);

	return ResponseEntity.ok(
		ApiResponse.success("Update folder successfully", updated, httpServletRequest.getRequestURI())
	);
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<ApiResponse<String>> deleteFolder(
	    @PathVariable("folderId") String folderId,
	    HttpServletRequest httpServletRequest
    ) {
	folderService.deleteFolder(folderId);
	return ResponseEntity.ok(ApiResponse.success("Delete folder successfully", httpServletRequest.getRequestURI()));
    }

    @GetMapping("/project/{projectId}")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
        @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getFoldersByProject(
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

    @PostMapping("/project/{projectId}/with-acl")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    public ResponseEntity<ApiResponse<CreateFolderWithAclResponse>> createFolderWithAcl(
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
                .body(ApiResponse.success("Create folder with ACL successfully", response, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/project/{projectId}/paths/children")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
        @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
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

    @GetMapping("/project/{projectId}/paths/search")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
        @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
    public ResponseEntity<ApiResponse<List<FolderPathNodeResponse>>> searchFolderPaths(
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
                ApiResponse.success("Search folder paths successfully", response, httpServletRequest.getRequestURI())
        );
    }

    // ─── New endpoints ─────────────────────────────────────────────────────────

    /**
     * GET /api/v1/folders/project/{projectId}/members-for-acl
     * Returns all project members (owner + members) available for Folder ACL selection.
     */
    @GetMapping("/project/{projectId}/members-for-acl")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
        @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
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

    /**
     * GET /api/v1/folders/{folderId}/acl
     * Returns all ACL entries for a specific folder.
     */
    @GetMapping("/{folderId}/acl")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
        @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
    public ResponseEntity<ApiResponse<List<FolderAclItemResponse>>> getFolderAcl(
            @PathVariable("folderId") String folderId,
            HttpServletRequest httpServletRequest
    ) {
        List<FolderAclItemResponse> response = folderService.getFolderAcl(
                folderId,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Get folder ACL successfully", response, httpServletRequest.getRequestURI())
        );
    }

    /**
     * PUT /api/v1/folders/{folderId}/acl/{userId}
     * Upsert (create or update) a user's permission on a folder.
     * Body: { "permission": "VIEW" | "EDIT" | "DENIED" }
     */
    @PutMapping("/{folderId}/acl/{userId}")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    public ResponseEntity<ApiResponse<FolderAclItemResponse>> upsertFolderAcl(
            @PathVariable("folderId") String folderId,
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpsertFolderAclRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FolderAclItemResponse response = folderService.upsertFolderAcl(
                folderId,
                userId,
                request,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Upsert folder ACL successfully", response, httpServletRequest.getRequestURI())
        );
    }

    // ─── Folder rename & delete with permission check ───────────────────────────

    /**
     * PATCH /api/v1/folders/{folderId}/rename
     * Đổi tên folder.
     * Yêu cầu actor có quyền WRITE (bit 2 – các giá trị hợp lệ: 2, 3, 6, 7).
     * TENANT_ADMIN và project owner được bypass kiểm tra quyền.
     */
    @PatchMapping("/{folderId}/rename")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    @RequireFolderPermission(RequireFolderPermission.FolderAction.WRITE)
    public ResponseEntity<ApiResponse<FolderResponse>> renameFolder(
            @PathVariable("folderId") String folderId,
            @Valid @RequestBody RenameFolderRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FolderResponse response = folderService.renameFolder(
                folderId,
                request,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Rename folder successfully", response, httpServletRequest.getRequestURI())
        );
    }

    /**
     * DELETE /api/v1/folders/{folderId}/actor
     * Xóa folder (với kiểm tra quyền actor).
     * Yêu cầu actor có quyền DELETE (bit 4 – các giá trị hợp lệ: 4, 5, 6, 7).
     * TENANT_ADMIN và project owner được bypass kiểm tra quyền.
     */
    @DeleteMapping("/{folderId}/actor")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    @RequireFolderPermission(RequireFolderPermission.FolderAction.DELETE)
    public ResponseEntity<ApiResponse<String>> deleteFolderByActor(
            @PathVariable("folderId") String folderId,
            HttpServletRequest httpServletRequest
    ) {
        folderService.deleteFolderByActor(
                folderId,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Delete folder successfully", httpServletRequest.getRequestURI())
        );
    }
}

