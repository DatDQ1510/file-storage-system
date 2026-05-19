package com.java.file_storage_system.controller;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.custom.RequireFilePermission;
import com.java.file_storage_system.custom.RequireFilePermission.FileAction;
import com.java.file_storage_system.custom.RequireRole;
import com.java.file_storage_system.dto.file.*;
import com.java.file_storage_system.dto.starred.StarredFileResponse;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.StarService;
import com.java.file_storage_system.service.FileService;
import com.java.file_storage_system.service.FileShareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;
    private final FileShareService fileShareService;
    private final StarService starService;
    private final UserContext userContext;

    @GetMapping
    @RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    @RequireFilePermission(FileAction.READ)
    public ResponseEntity<ApiResponse<List<FileResponse>>> getAllFiles(
            @RequestParam(value = "folderId", required = false) String folderId,
            HttpServletRequest httpServletRequest) {
        List<FileResponse> files;
        if (folderId != null && !folderId.trim().isEmpty()) {
            files = fileService.getFilesByFolder(folderId);
        } else {
            files = fileService.getAllFiles();
        }
        return ResponseEntity.ok(ApiResponse.success("Get files successfully", files, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{fileId}")
    @RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    @RequireFilePermission(FileAction.READ)
    public ResponseEntity<ApiResponse<FileResponse>> getFileById(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        FileResponse file = fileService.getFileById(fileId);
        return ResponseEntity.ok(ApiResponse.success("Get file successfully", file, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.WRITE)
    public ResponseEntity<ApiResponse<FileResponse>> createFile(
            @Valid @RequestBody CreateFileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileResponse created = fileService.createFile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create file successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{fileId}")
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.WRITE)
    public ResponseEntity<ApiResponse<FileResponse>> updateFile(
            @PathVariable("fileId") String fileId,
            @Valid @RequestBody UpdateFileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileResponse updated = fileService.updateFile(fileId, request);
        return ResponseEntity.ok(ApiResponse.success("Update file successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{fileId}")
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.DELETE)
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        fileService.deleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.success("Move file to recycle bin successfully", httpServletRequest.getRequestURI()));
    }

    @PatchMapping("/{fileId}")
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.WRITE)
    public ResponseEntity<ApiResponse<FileResponse>> renameFile(
            @PathVariable("fileId") String fileId,
            @Valid @RequestBody RenameFileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileResponse updated = fileService.renameFile(fileId, request.nameFile());
        return ResponseEntity.ok(ApiResponse.success("Rename file successfully", updated, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/recycle-bin")
    @RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    public ResponseEntity<ApiResponse<List<RecycleBinFileResponse>>> getRecycleBinFiles(
            HttpServletRequest httpServletRequest
    ) {
        List<com.java.file_storage_system.dto.file.RecycleBinFileResponse> items = fileService.getRecycleBinFiles();
        return ResponseEntity.ok(ApiResponse.success("Get recycle bin files successfully", items, httpServletRequest.getRequestURI()));
    }

    @PatchMapping("/{fileId}/restore")
    @RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    public ResponseEntity<ApiResponse<String>> restoreFile(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        fileService.restoreFile(fileId);
        return ResponseEntity.ok(ApiResponse.success("Restore file successfully", httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{fileId}/permanent")
    @RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    public ResponseEntity<ApiResponse<String>> permanentlyDeleteFile(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        fileService.permanentlyDeleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.success("Delete file permanently successfully", httpServletRequest.getRequestURI()));
    }
  
    @DeleteMapping("/recycle-bin/empty")
    @RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    public ResponseEntity<ApiResponse<String>> emptyRecycleBin(
            HttpServletRequest httpServletRequest
    ) {
        fileService.emptyRecycleBin();
        return ResponseEntity.ok(ApiResponse.success("Empty recycle bin successfully", httpServletRequest.getRequestURI()));
    }

    @PostMapping("/{fileId}/star")
    @RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    @RequireFilePermission(FileAction.READ)
    public ResponseEntity<ApiResponse<StarredFileResponse>> starFile(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        var response = starService.starFile(fileId, userContext.getId(), userContext.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Star file successfully", response, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{fileId}/star")
    @RequireRole({UserRole.USER, UserRole.TENANT_ADMIN})
    @RequireFilePermission(FileAction.READ)
    public ResponseEntity<ApiResponse<String>> unstarFile(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        starService.unstarFile(fileId, userContext.getId());
        return ResponseEntity.ok(ApiResponse.success("Unstar file successfully", httpServletRequest.getRequestURI()));
    }

    // ========== File Share Endpoints ==========

    /**
     * Share file với user khác
     * POST /api/v1/files/{fileId}/share
     * 
     * Request body:
     * {
     *   "sharedWithUserId": "uuid",
     *   "permission": "VIEW|COMMENT|EDIT",
     *   "expiresAt": "2026-12-31T23:59:59"  // optional
     * }
     */
    @PostMapping("/{fileId}/share")
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.READ)  // Minimal check - service will validate ownership
    public ResponseEntity<ApiResponse<FileShareResponse>> shareFile(
            @PathVariable("fileId") String fileId,
            @Valid @RequestBody ShareFileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileShareResponse response = fileShareService.shareFile(fileId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Share file successfully", response, httpServletRequest.getRequestURI()));
    }

    /**
     * Revoke file share (xoá share)
     * DELETE /api/v1/files/{fileId}/share/{sharedWithUserId}
     */
    @DeleteMapping("/{fileId}/share/{sharedWithUserId}")
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.READ)  // Minimal check - service will validate ownership
    public ResponseEntity<ApiResponse<String>> unshareFile(
            @PathVariable("fileId") String fileId,
            @PathVariable("sharedWithUserId") String sharedWithUserId,
            HttpServletRequest httpServletRequest
    ) {
        fileShareService.unshareFile(fileId, sharedWithUserId);
        return ResponseEntity.ok(ApiResponse.success("Unshare file successfully", httpServletRequest.getRequestURI()));
    }

    /**
     * Update share permission
     * PUT /api/v1/files/{fileId}/share/{sharedWithUserId}
     */
    @PutMapping("/{fileId}/share/{sharedWithUserId}")
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.READ)  // Minimal check - service will validate ownership
    public ResponseEntity<ApiResponse<FileShareResponse>> updateFileShare(
            @PathVariable("fileId") String fileId,
            @PathVariable("sharedWithUserId") String sharedWithUserId,
            @Valid @RequestBody ShareFileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileShareResponse response = fileShareService.updateFileShare(fileId, sharedWithUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Update share successfully", response, httpServletRequest.getRequestURI()));
    }

    /**
     * Lấy tất cả file shares của file (ai được share file này)
     * GET /api/v1/files/{fileId}/shares
     */
    @GetMapping("/{fileId}/shares")
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.READ)  // Minimal check - service will validate ownership
    public ResponseEntity<ApiResponse<List<FileShareResponse>>> getFileShares(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        List<FileShareResponse> responses = fileShareService.getFileShares(fileId);
        return ResponseEntity.ok(ApiResponse.success("Get file shares successfully", responses, httpServletRequest.getRequestURI()));
    }

    /**
     * Lấy file share details
     * GET /api/v1/files/{fileId}/share/{sharedWithUserId}
     */
    @GetMapping("/{fileId}/share/{sharedWithUserId}")
    @RequireRole(UserRole.USER)
    @RequireFilePermission(FileAction.READ)
    public ResponseEntity<ApiResponse<FileShareResponse>> getFileShare(
            @PathVariable("fileId") String fileId,
            @PathVariable("sharedWithUserId") String sharedWithUserId,
            HttpServletRequest httpServletRequest
    ) {
        FileShareResponse response = fileShareService.getFileShare(fileId, sharedWithUserId);
        return ResponseEntity.ok(ApiResponse.success("Get file share successfully", response, httpServletRequest.getRequestURI()));
    }
}
