package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.fileVersion.CreateFileVersionRequest;
import com.java.file_storage_system.dto.fileVersion.FileVersionResponse;
import com.java.file_storage_system.dto.fileVersion.UpdateFileVersionRequest;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.FileVersionService;
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
@RequestMapping("/api/v1/file-versions")
public class FileVersionController {

    private final FileVersionService fileVersionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileVersionResponse>>> getAllFileVersions(HttpServletRequest httpServletRequest) {
        List<FileVersionResponse> versions = fileVersionService.getAllFileVersions();
        return ResponseEntity.ok(ApiResponse.success("Get file versions successfully", versions, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{fileVersionId}")
    public ResponseEntity<ApiResponse<FileVersionResponse>> getFileVersionById(
            @PathVariable("fileVersionId") String fileVersionId,
            HttpServletRequest httpServletRequest
    ) {
        FileVersionResponse fileVersion = fileVersionService.getFileVersionById(fileVersionId);
        return ResponseEntity.ok(ApiResponse.success("Get file version successfully", fileVersion, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FileVersionResponse>> createFileVersion(
            @Valid @RequestBody CreateFileVersionRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileVersionResponse created = fileVersionService.createFileVersion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create file version successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{fileVersionId}")
    public ResponseEntity<ApiResponse<FileVersionResponse>> updateFileVersion(
            @PathVariable("fileVersionId") String fileVersionId,
            @Valid @RequestBody UpdateFileVersionRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileVersionResponse updated = fileVersionService.updateFileVersion(fileVersionId, request);
        return ResponseEntity.ok(ApiResponse.success("Update file version successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{fileVersionId}")
    public ResponseEntity<ApiResponse<String>> deleteFileVersion(
            @PathVariable("fileVersionId") String fileVersionId,
            HttpServletRequest httpServletRequest
    ) {
        fileVersionService.deleteFileVersion(fileVersionId);
        return ResponseEntity.ok(ApiResponse.success("Delete file version successfully", httpServletRequest.getRequestURI()));
    }
}
