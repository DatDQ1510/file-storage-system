package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.file.CreateFileRequest;
import com.java.file_storage_system.dto.file.FileResponse;
import com.java.file_storage_system.dto.file.UpdateFileRequest;
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
    public ResponseEntity<ApiResponse<List<FileResponse>>> getAllFiles(HttpServletRequest httpServletRequest) {
        List<FileResponse> files = fileService.getAllFiles();
        return ResponseEntity.ok(ApiResponse.success("Get files successfully", files, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponse>> getFileById(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        FileResponse file = fileService.getFileById(fileId);
        return ResponseEntity.ok(ApiResponse.success("Get file successfully", file, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FileResponse>> createFile(
            @Valid @RequestBody CreateFileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileResponse created = fileService.createFile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create file successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponse>> updateFile(
            @PathVariable("fileId") String fileId,
            @Valid @RequestBody UpdateFileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileResponse updated = fileService.updateFile(fileId, request);
        return ResponseEntity.ok(ApiResponse.success("Update file successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable("fileId") String fileId,
            HttpServletRequest httpServletRequest
    ) {
        fileService.deleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.success("Delete file successfully", httpServletRequest.getRequestURI()));
    }
}
