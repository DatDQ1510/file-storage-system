package com.java.file_storage_system.controller;

import com.java.file_storage_system.entity.FileEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.FileService;
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
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileEntity>>> getAllFiles(HttpServletRequest httpServletRequest) {
        List<FileEntity> files = fileService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Get files successfully", files, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileEntity>> getFileById(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        FileEntity file = fileService.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));

        return ResponseEntity.ok(ApiResponse.success("Get file successfully", file, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FileEntity>> createFile(
            @Valid @RequestBody FileEntity request,
            HttpServletRequest httpServletRequest
    ) {
        // Prevent overwrite-by-id when creating new records.
        request.setId(null);
        FileEntity created = fileService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create file successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileEntity>> updateFile(
            @PathVariable("fileId") String fileId,
            @Valid @RequestBody FileEntity request,
            HttpServletRequest httpServletRequest
    ) {
        fileService.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));

        request.setId(fileId);
        FileEntity updated = fileService.save(request);

        return ResponseEntity.ok(ApiResponse.success("Update file successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        fileService.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));

        fileService.deleteById(fileId);
        return ResponseEntity.ok(ApiResponse.success("Delete file successfully", httpServletRequest.getRequestURI()));
    }
}
