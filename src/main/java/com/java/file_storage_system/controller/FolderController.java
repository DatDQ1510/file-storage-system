package com.java.file_storage_system.controller;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.custom.RequireFolderPermission;
import com.java.file_storage_system.custom.RequireRole;
import com.java.file_storage_system.dto.folder.FolderAclItemResponse;
import com.java.file_storage_system.dto.folder.FolderResponse;
import com.java.file_storage_system.dto.folder.RenameFolderRequest;
import com.java.file_storage_system.dto.folder.UpsertFolderAclRequest;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.FolderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Folder-specific endpoints (không liên quan đến project hierarchy).
 * Resource hierarchy:
 *   GET    /api/v1/folders/{folderId}                   → lấy 1 folder theo id
 *   GET    /api/v1/folders/{folderId}/children           → lấy direct children theo parentId
 *   PATCH  /api/v1/folders/{folderId}                   → đổi tên folder (yêu cầu WRITE)
 *   DELETE /api/v1/folders/{folderId}                   → xóa folder (yêu cầu DELETE)
 *   GET    /api/v1/folders/{folderId}/acl               → xem ACL của folder
 *   PUT    /api/v1/folders/{folderId}/acl/{userId}      → upsert ACL entry
 *
 * Project-scoped folder endpoints nằm trong ProjectController (/api/v1/projects/{projectId}/folders/*).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/folders")
public class FolderController {

    private final FolderService folderService;
    private final UserContext userContext;

    // ─── Single folder ────────────────────────────────────────────────────────

    /**
     * GET /api/v1/folders/{folderId}
     * Lấy thông tin chi tiết của một folder.
     */
    @GetMapping("/{folderId}")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
    public ResponseEntity<ApiResponse<FolderResponse>> getFolderById(
            @PathVariable("folderId") String folderId,
            HttpServletRequest httpServletRequest
    ) {
        FolderResponse folder = folderService.getFolderById(folderId);
        return ResponseEntity.ok(
                ApiResponse.success("Get folder successfully", folder, httpServletRequest.getRequestURI())
        );
    }

    /**
     * GET /api/v1/folders/{folderId}/children
     * Lấy danh sách folder con trực tiếp (direct children) theo parentId.
     * Hỗ trợ duyệt cây thư mục theo cấu trúc id.
     */
    @GetMapping("/{folderId}/children")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    @RequireFolderPermission(RequireFolderPermission.FolderAction.READ)
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getChildFolders(
            @PathVariable("folderId") String folderId,
            @RequestParam("projectId") String projectId,
            HttpServletRequest httpServletRequest
    ) {
        List<FolderResponse> children = folderService.getFoldersByParentId(
                projectId,
                folderId,
                userContext.getId(),
                userContext.getRole(),
                userContext.getTenantId()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Get child folders successfully", children, httpServletRequest.getRequestURI())
        );
    }

    // ─── Rename (PATCH) ───────────────────────────────────────────────────────

    /**
     * PATCH /api/v1/folders/{folderId}
     * Đổi tên folder.
     * Yêu cầu actor có quyền WRITE (bit 2).
     * TENANT_ADMIN và project owner được bypass kiểm tra quyền.
     */
    @PatchMapping("/{folderId}")
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

    // ─── Delete ───────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/folders/{folderId}
     * Xóa folder.
     * Yêu cầu actor có quyền DELETE (bit 4).
     * TENANT_ADMIN và project owner được bypass kiểm tra quyền.
     */
    @DeleteMapping("/{folderId}")
    @RequireRole({UserRole.TENANT_ADMIN, UserRole.USER})
    @RequireFolderPermission(RequireFolderPermission.FolderAction.DELETE)
    public ResponseEntity<ApiResponse<String>> deleteFolder(
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

    // ─── ACL ──────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/folders/{folderId}/acl
     * Xem tất cả ACL entries của một folder.
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
     * Upsert (tạo hoặc cập nhật) permission của một user trên folder.
     * Body: { "permission": 1-7 (bitmask) }
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
}
