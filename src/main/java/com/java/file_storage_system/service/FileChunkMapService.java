package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.filechunkmap.CreateFileChunkMapRequest;
import com.java.file_storage_system.dto.filechunkmap.FileChunkMapResponse;
import com.java.file_storage_system.dto.filechunkmap.UpdateFileChunkMapRequest;
import com.java.file_storage_system.entity.FileChunkMapEntity;

import java.util.List;

public interface FileChunkMapService extends BaseService<FileChunkMapEntity> {

	List<FileChunkMapResponse> getAllFileChunkMaps();

	FileChunkMapResponse getFileChunkMapById(String fileChunkMapId);

	FileChunkMapResponse createFileChunkMap(CreateFileChunkMapRequest request);

	FileChunkMapResponse updateFileChunkMap(String fileChunkMapId, UpdateFileChunkMapRequest request);

	void deleteFileChunkMap(String fileChunkMapId);
}
