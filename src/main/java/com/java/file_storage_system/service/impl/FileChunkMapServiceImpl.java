package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.filechunkmap.CreateFileChunkMapRequest;
import com.java.file_storage_system.dto.filechunkmap.FileChunkMapResponse;
import com.java.file_storage_system.dto.filechunkmap.UpdateFileChunkMapRequest;
import com.java.file_storage_system.entity.FileChunkMapEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FileChunkMapRepository;
import com.java.file_storage_system.service.FileChunkMapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FileChunkMapServiceImpl extends BaseServiceImpl<FileChunkMapEntity, FileChunkMapRepository> implements FileChunkMapService {

    public FileChunkMapServiceImpl(FileChunkMapRepository repository) {
        super(repository);
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
        FileChunkMapEntity item = new FileChunkMapEntity();
        item.setVersionId(request.versionId());
        item.setChunkId(request.chunkId());
        item.setOrderIndex(request.orderIndex());

        FileChunkMapEntity saved = repository.save(item);
        return toFileChunkMapResponse(saved);
    }

    @Override
    @Transactional
    public FileChunkMapResponse updateFileChunkMap(String fileChunkMapId, UpdateFileChunkMapRequest request) {
        FileChunkMapEntity existing = repository.findById(fileChunkMapId)
                .orElseThrow(() -> new ResourceNotFoundException("FileChunkMap not found with id: " + fileChunkMapId));

        existing.setVersionId(request.versionId());
        existing.setChunkId(request.chunkId());
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
                item.getVersionId(),
                item.getChunkId(),
                item.getOrderIndex(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
