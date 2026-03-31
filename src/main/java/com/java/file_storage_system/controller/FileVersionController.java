package com.java.file_storage_system.controller;

import com.java.file_storage_system.entity.FileVersionEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
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
    public ResponseEntity<ApiResponse<List<FileVersionEntity>>> getAllFileVersions(HttpServletRequest httpServletRequest) {
        List<FileVersionEntity> versions = fileVersionService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Get file versions successfully", versions, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{fileVersionId}")
    public ResponseEntity<ApiResponse<FileVersionEntity>> getFileVersionById(
            @PathVariable("fileVersionId") String fileVersionId,
            HttpServletRequest httpServletRequest
    ) {
        FileVersionEntity fileVersion = fileVersionService.findById(fileVersionId)
                .orElseThrow(() -> ResourceNotFoundException.byField("FileVersion", "id", fileVersionId));

        return ResponseEntity.ok(ApiResponse.success("Get file version successfully", fileVersion, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FileVersionEntity>> createFileVersion(
            @Valid @RequestBody FileVersionEntity request,
            HttpServletRequest httpServletRequest
    ) {
        // Prevent overwrite-by-id when creating new records.
        request.setId(null);
        FileVersionEntity created = fileVersionService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create file version successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{fileVersionId}")
    public ResponseEntity<ApiResponse<FileVersionEntity>> updateFileVersion(
            @PathVariable("fileVersionId") String fileVersionId,
            @Valid @RequestBody FileVersionEntity request,
            HttpServletRequest httpServletRequest
    ) {
        fileVersionService.findById(fileVersionId)
                .orElseThrow(() -> ResourceNotFoundException.byField("FileVersion", "id", fileVersionId));

        request.setId(fileVersionId);
        FileVersionEntity updated = fileVersionService.save(request);

        return ResponseEntity.ok(ApiResponse.success("Update file version successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{fileVersionId}")
    public ResponseEntity<ApiResponse<String>> deleteFileVersion(
            @PathVariable("fileVersionId") String fileVersionId,
            HttpServletRequest httpServletRequest
    ) {
        fileVersionService.findById(fileVersionId)
                .orElseThrow(() -> ResourceNotFoundException.byField("FileVersion", "id", fileVersionId));

        fileVersionService.deleteById(fileVersionId);
        return ResponseEntity.ok(ApiResponse.success("Delete file version successfully", httpServletRequest.getRequestURI()));
    }
}
