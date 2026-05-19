package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.chunk.ChunkResponse;
import com.java.file_storage_system.dto.chunk.CreateChunkRequest;
import com.java.file_storage_system.dto.chunk.UpdateChunkRequest;
import com.java.file_storage_system.config.MinioProperties;
import com.java.file_storage_system.entity.ChunkEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.ChunkRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.service.ChunkService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChunkServiceImpl extends BaseServiceImpl<ChunkEntity, ChunkRepository> implements ChunkService {

    private final TenantRepository tenantRepository;
    private final MinioProperties minioProperties;

    @Override
    @Transactional(readOnly = true)
    public List<ChunkResponse> getAllChunks() {
        return repository.findAll().stream().map(this::toChunkResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChunkResponse getChunkById(String chunkId) {
        ChunkEntity chunk = repository.findById(chunkId)
                .orElseThrow(() -> new ResourceNotFoundException("Chunk not found with id: " + chunkId));
        return toChunkResponse(chunk);
    }

    @Override
    @Transactional
    public ChunkResponse createChunk(CreateChunkRequest request) {
        TenantEntity tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + request.tenantId()));

        ChunkEntity chunk = new ChunkEntity();
        String chunkId = UUID.randomUUID().toString();
        chunk.setId(chunkId);
        chunk.setChunkId(chunkId);
        chunk.setSize(Math.round(request.sizeChunk()));
        String[] bucketAndObject = parseMinioLocation(request.minIOUrl());
        chunk.setBucket(bucketAndObject[0]);
        chunk.setObjectName(bucketAndObject[1]);
        chunk.setMinIOUrl(buildMinioUrl(bucketAndObject[0], bucketAndObject[1]));
        chunk.setChunkHash(request.chunkHash());
        chunk.setStatus("DONE");
        chunk.setTenant(tenant);

        ChunkEntity saved = repository.save(chunk);
        return toChunkResponse(saved);
    }

    @Override
    @Transactional
    public ChunkResponse updateChunk(String chunkId, UpdateChunkRequest request) {
        ChunkEntity existing = repository.findById(chunkId)
                .orElseThrow(() -> new ResourceNotFoundException("Chunk not found with id: " + chunkId));

        TenantEntity tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + request.tenantId()));

        existing.setSize(Math.round(request.sizeChunk()));
        String[] bucketAndObject = parseMinioLocation(request.minIOUrl());
        existing.setBucket(bucketAndObject[0]);
        existing.setObjectName(bucketAndObject[1]);
        existing.setMinIOUrl(buildMinioUrl(bucketAndObject[0], bucketAndObject[1]));
        existing.setChunkId(existing.getChunkId() == null || existing.getChunkId().isBlank() ? existing.getId() : existing.getChunkId());
        existing.setChunkHash(request.chunkHash());
        existing.setStatus("DONE");
        existing.setTenant(tenant);

        ChunkEntity updated = repository.save(existing);
        return toChunkResponse(updated);
    }

    @Override
    @Transactional
    public void deleteChunk(String chunkId) {
        if (!repository.existsById(chunkId)) {
            throw new ResourceNotFoundException("Chunk not found with id: " + chunkId);
        }
        repository.deleteById(chunkId);
    }

    private ChunkResponse toChunkResponse(ChunkEntity chunk) {
        return new ChunkResponse(
                chunk.getId(),
                chunk.getSize() == null ? null : chunk.getSize().doubleValue(),
                buildMinioUrl(chunk),
                chunk.getChunkHash(),
                chunk.getTenant() != null ? chunk.getTenant().getId() : null,
                chunk.getCreatedAt(),
                chunk.getUpdatedAt()
        );
    }

    private String buildMinioUrl(ChunkEntity chunk) {
        if (chunk.getMinIOUrl() != null && !chunk.getMinIOUrl().isBlank()) {
            return chunk.getMinIOUrl();
        }

        if (chunk.getBucket() == null || chunk.getObjectName() == null) {
            return null;
        }
        return chunk.getBucket() + "/" + chunk.getObjectName();
    }

    private String buildMinioUrl(String bucket, String objectName) {
        if (bucket == null || bucket.isBlank() || objectName == null || objectName.isBlank()) {
            return null;
        }

        return bucket + "/" + objectName;
    }

    private String[] parseMinioLocation(String minioUrl) {
        if (minioUrl == null || minioUrl.isBlank()) {
            throw new IllegalArgumentException("minIOUrl is required");
        }

        String value = minioUrl.trim();
        String withoutScheme = value;
        int schemeIndex = value.indexOf("://");
        if (schemeIndex >= 0) {
            withoutScheme = value.substring(schemeIndex + 3);
            int firstSlash = withoutScheme.indexOf('/');
            if (firstSlash >= 0 && firstSlash + 1 < withoutScheme.length()) {
                withoutScheme = withoutScheme.substring(firstSlash + 1);
            }
        }

        String normalized = withoutScheme.startsWith("/") ? withoutScheme.substring(1) : withoutScheme;
        int slashIndex = normalized.indexOf('/');
        if (slashIndex <= 0 || slashIndex >= normalized.length() - 1) {
            return new String[] { minioProperties.getBucket(), normalized };
        }

        String bucket = normalized.substring(0, slashIndex);
        String objectName = normalized.substring(slashIndex + 1);
        return new String[] { bucket, objectName };
    }
}
