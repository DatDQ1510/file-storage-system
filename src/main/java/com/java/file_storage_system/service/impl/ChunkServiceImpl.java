package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.chunk.ChunkResponse;
import com.java.file_storage_system.dto.chunk.CreateChunkRequest;
import com.java.file_storage_system.dto.chunk.UpdateChunkRequest;
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

@Service
@RequiredArgsConstructor
public class ChunkServiceImpl extends BaseServiceImpl<ChunkEntity, ChunkRepository> implements ChunkService {

    private final TenantRepository tenantRepository;

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
        chunk.setSizeChunk(request.sizeChunk());
        chunk.setMinIOUrl(request.minIOUrl());
        chunk.setChunkHash(request.chunkHash());
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

        existing.setSizeChunk(request.sizeChunk());
        existing.setMinIOUrl(request.minIOUrl());
        existing.setChunkHash(request.chunkHash());
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
                chunk.getSizeChunk(),
                chunk.getMinIOUrl(),
                chunk.getChunkHash(),
                chunk.getTenant() != null ? chunk.getTenant().getId() : null,
                chunk.getCreatedAt(),
                chunk.getUpdatedAt()
        );
    }
}
