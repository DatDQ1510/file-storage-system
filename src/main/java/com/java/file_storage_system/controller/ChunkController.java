package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.chunk.ChunkResponse;
import com.java.file_storage_system.dto.chunk.CreateChunkRequest;
import com.java.file_storage_system.dto.chunk.UpdateChunkRequest;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.ChunkService;
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
@RequestMapping("/api/v1/chunks")
public class ChunkController {

    private final ChunkService chunkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChunkResponse>>> getAllChunks(HttpServletRequest httpServletRequest) {
        List<ChunkResponse> chunks = chunkService.getAllChunks();
        return ResponseEntity.ok(ApiResponse.success("Get chunks successfully", chunks, httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{chunkId}")
    public ResponseEntity<ApiResponse<ChunkResponse>> getChunkById(
            @PathVariable("chunkId") String chunkId,
            HttpServletRequest httpServletRequest
    ) {
        ChunkResponse chunk = chunkService.getChunkById(chunkId);
        return ResponseEntity.ok(ApiResponse.success("Get chunk successfully", chunk, httpServletRequest.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChunkResponse>> createChunk(
            @Valid @RequestBody CreateChunkRequest request,
            HttpServletRequest httpServletRequest
    ) {
        ChunkResponse created = chunkService.createChunk(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create chunk successfully", created, httpServletRequest.getRequestURI()));
    }

    @PutMapping("/{chunkId}")
    public ResponseEntity<ApiResponse<ChunkResponse>> updateChunk(
            @PathVariable("chunkId") String chunkId,
            @Valid @RequestBody UpdateChunkRequest request,
            HttpServletRequest httpServletRequest
    ) {
        ChunkResponse updated = chunkService.updateChunk(chunkId, request);
        return ResponseEntity.ok(ApiResponse.success("Update chunk successfully", updated, httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{chunkId}")
    public ResponseEntity<ApiResponse<String>> deleteChunk(
            @PathVariable("chunkId") String chunkId,
            HttpServletRequest httpServletRequest
    ) {
        chunkService.deleteChunk(chunkId);
        return ResponseEntity.ok(ApiResponse.success("Delete chunk successfully", httpServletRequest.getRequestURI()));
    }
}
