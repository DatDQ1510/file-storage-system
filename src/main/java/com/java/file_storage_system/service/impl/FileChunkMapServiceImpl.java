package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.filechunkmap.CreateFileChunkMapRequest;
import com.java.file_storage_system.dto.filechunkmap.FileChunkMapResponse;
import com.java.file_storage_system.dto.filechunkmap.UpdateFileChunkMapRequest;
import com.java.file_storage_system.entity.ChunkEntity;
import com.java.file_storage_system.entity.FileChunkMapEntity;
import com.java.file_storage_system.entity.FileVersionEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.ChunkRepository;
import com.java.file_storage_system.repository.FileChunkMapRepository;
import com.java.file_storage_system.repository.FileVersionRepository;
import com.java.file_storage_system.service.FileChunkMapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FileChunkMapServiceImpl extends BaseServiceImpl<FileChunkMapEntity, FileChunkMapRepository> implements FileChunkMapService {

    private final FileVersionRepository fileVersionRepository;
    private final ChunkRepository chunkRepository;

    public FileChunkMapServiceImpl(
            FileChunkMapRepository repository,
            FileVersionRepository fileVersionRepository,
            ChunkRepository chunkRepository
    ) {
        super(repository);
        this.fileVersionRepository = fileVersionRepository;
        this.chunkRepository = chunkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileChunkMapResponse> getAllFileChunkMaps() {
        return repository.findAll().stream().map(this::toFileChunkMapResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileChunkMapResponse getFileChunkMapById(String fileChunkMapId) {
        FileChunkMapEntity item = repository.findById(fileChunkMapId)
                .orElseThrow(() -> new ResourceNotFoundException("FileChunkMap not found with id: " + fileChunkMapId));
        return toFileChunkMapResponse(item);
    }

    @Override
    @Transactional
    public FileChunkMapResponse createFileChunkMap(CreateFileChunkMapRequest request) {
        FileVersionEntity fileVersion = fileVersionRepository.findById(request.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("FileVersion not found with id: " + request.versionId()));
        ChunkEntity chunk = chunkRepository.findById(request.chunkId())
            .orElseThrow(() -> new ResourceNotFoundException("Chunk not found with id: " + request.chunkId()));

        FileChunkMapEntity item = new FileChunkMapEntity();
        item.setFileVersion(fileVersion);
        item.setChunk(chunk);
        item.setOrderIndex(request.orderIndex());
        item.setStatus("PENDING");

        FileChunkMapEntity saved = repository.save(item);
        return toFileChunkMapResponse(saved);
    }

    @Override
    @Transactional
    public FileChunkMapResponse updateFileChunkMap(String fileChunkMapId, UpdateFileChunkMapRequest request) {
        FileChunkMapEntity existing = repository.findById(fileChunkMapId)
                .orElseThrow(() -> new ResourceNotFoundException("FileChunkMap not found with id: " + fileChunkMapId));

        FileVersionEntity fileVersion = fileVersionRepository.findById(request.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("FileVersion not found with id: " + request.versionId()));
        ChunkEntity chunk = chunkRepository.findById(request.chunkId())
            .orElseThrow(() -> new ResourceNotFoundException("Chunk not found with id: " + request.chunkId()));

        existing.setFileVersion(fileVersion);
        existing.setChunk(chunk);
        existing.setOrderIndex(request.orderIndex());

        FileChunkMapEntity updated = repository.save(existing);
        return toFileChunkMapResponse(updated);
    }

    @Override
    @Transactional
    public void deleteFileChunkMap(String fileChunkMapId) {
        if (!repository.existsById(fileChunkMapId)) {
            throw new ResourceNotFoundException("FileChunkMap not found with id: " + fileChunkMapId);
        }
        repository.deleteById(fileChunkMapId);
    }

    private FileChunkMapResponse toFileChunkMapResponse(FileChunkMapEntity item) {
        return new FileChunkMapResponse(
                item.getId(),
                item.getFileVersion() != null ? item.getFileVersion().getId() : null,
                item.getChunk() != null ? item.getChunk().getId() : null,
                item.getOrderIndex(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
