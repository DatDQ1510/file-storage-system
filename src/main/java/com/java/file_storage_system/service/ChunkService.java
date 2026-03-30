package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.chunk.ChunkResponse;
import com.java.file_storage_system.dto.chunk.CreateChunkRequest;
import com.java.file_storage_system.dto.chunk.UpdateChunkRequest;
import com.java.file_storage_system.entity.ChunkEntity;

import java.util.List;

public interface ChunkService extends BaseService<ChunkEntity> {

	List<ChunkResponse> getAllChunks();

	ChunkResponse getChunkById(String chunkId);

	ChunkResponse createChunk(CreateChunkRequest request);

	ChunkResponse updateChunk(String chunkId, UpdateChunkRequest request);

	void deleteChunk(String chunkId);
}
