package com.java.file_storage_system.controller;

import com.java.file_storage_system.dto.folder.CreateFolderRequest;
import com.java.file_storage_system.dto.folder.FolderResponse;
import com.java.file_storage_system.dto.folder.UpdateFolderRequest;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.FolderService;
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
@RequestMapping("/api/v1/folders")
public class FolderController {

    private final FolderService folderService;

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
}
