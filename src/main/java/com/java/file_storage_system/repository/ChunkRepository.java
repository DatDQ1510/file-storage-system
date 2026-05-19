package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.ChunkEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChunkRepository extends BaseRepository<ChunkEntity> {
	Optional<ChunkEntity> findFirstByTenant_IdAndChunkHashAndSizeAndStatus(
			String tenantId,
			String chunkHash,
			Long size,
			String status
	);

	Optional<ChunkEntity> findTopByTenant_IdAndChunkHashOrderByCreatedAtDesc(String tenantId, String chunkHash);

	List<ChunkEntity> findByTenant_IdAndChunkHashInAndStatus(String tenantId, Collection<String> chunkHashes, String status);

	long countByTenant_IdAndChunkHashInAndStatus(String tenantId, Collection<String> chunkHashes, String status);
}
