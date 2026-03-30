package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.filechunkmap.CreateFileChunkMapRequest;
import com.java.file_storage_system.dto.filechunkmap.FileChunkMapResponse;
import com.java.file_storage_system.dto.filechunkmap.UpdateFileChunkMapRequest;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.FileChunkMapService;
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
@RequestMapping("/api/v1/file-chunk-maps")
public class FileChunkMapController {

    private final FileChunkMapService fileChunkMapService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileChunkMapResponse>>> getAllFileChunkMaps(HttpServletRequest httpServletRequest) {
        List<FileChunkMapResponse> items = fileChunkMapService.getAllFileChunkMaps();
        return ResponseEntity.ok(ApiResponse.success("Get file chunk maps successfully", items, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{fileChunkMapId}")
    public ResponseEntity<ApiResponse<FileChunkMapResponse>> getFileChunkMapById(
            @PathVariable("fileChunkMapId") String fileChunkMapId,
            HttpServletRequest httpServletRequest
    ) {
        FileChunkMapResponse item = fileChunkMapService.getFileChunkMapById(fileChunkMapId);
        return ResponseEntity.ok(ApiResponse.success("Get file chunk map successfully", item, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FileChunkMapResponse>> createFileChunkMap(
            @Valid @RequestBody CreateFileChunkMapRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileChunkMapResponse created = fileChunkMapService.createFileChunkMap(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create file chunk map successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{fileChunkMapId}")
    public ResponseEntity<ApiResponse<FileChunkMapResponse>> updateFileChunkMap(
            @PathVariable("fileChunkMapId") String fileChunkMapId,
            @Valid @RequestBody UpdateFileChunkMapRequest request,
            HttpServletRequest httpServletRequest
    ) {
        FileChunkMapResponse updated = fileChunkMapService.updateFileChunkMap(fileChunkMapId, request);
        return ResponseEntity.ok(ApiResponse.success("Update file chunk map successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{fileChunkMapId}")
    public ResponseEntity<ApiResponse<String>> deleteFileChunkMap(
            @PathVariable("fileChunkMapId") String fileChunkMapId,
            HttpServletRequest httpServletRequest
    ) {
        fileChunkMapService.deleteFileChunkMap(fileChunkMapId);
        return ResponseEntity.ok(ApiResponse.success("Delete file chunk map successfully", httpServletRequest.getRequestURI()));
    }
}
