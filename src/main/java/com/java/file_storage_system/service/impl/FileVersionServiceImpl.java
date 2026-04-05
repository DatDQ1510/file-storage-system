package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.dto.fileVersion.CreateFileVersionRequest;
import com.java.file_storage_system.dto.fileVersion.FileVersionResponse;
import com.java.file_storage_system.dto.fileVersion.UpdateFileVersionRequest;
import com.java.file_storage_system.entity.FileEntity;
import com.java.file_storage_system.entity.FileVersionEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FileRepository;
import com.java.file_storage_system.repository.FileVersionRepository;
import com.java.file_storage_system.service.FileVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileVersionServiceImpl extends BaseServiceImpl<FileVersionEntity, FileVersionRepository> implements FileVersionService {

    private final FileRepository fileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FileVersionResponse> getAllFileVersions() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileVersionResponse getFileVersionById(String fileVersionId) {
        return mapToResponse(findFileVersion(fileVersionId));
    }

    @Override
    @Transactional
    public FileVersionResponse createFileVersion(CreateFileVersionRequest request) {
        FileEntity file = findFile(request.fileId());

        FileVersionEntity fileVersion = new FileVersionEntity();
        fileVersion.setFileVersion(request.fileVersion());
        fileVersion.setFileHash(request.fileHash());
        fileVersion.setSizeFile(request.sizeFile());
        fileVersion.setFile(file);

        return mapToResponse(repository.save(fileVersion));
    }

    @Override
    @Transactional
    public FileVersionResponse updateFileVersion(String fileVersionId, UpdateFileVersionRequest request) {
        FileVersionEntity fileVersion = findFileVersion(fileVersionId);
        FileEntity file = findFile(request.fileId());

        fileVersion.setFileVersion(request.fileVersion());
        fileVersion.setFileHash(request.fileHash());
        fileVersion.setSizeFile(request.sizeFile());
        fileVersion.setFile(file);

        return mapToResponse(repository.save(fileVersion));
    }

    @Override
    @Transactional
    public void deleteFileVersion(String fileVersionId) {
        if (!repository.existsById(fileVersionId)) {
            throw ResourceNotFoundException.byField("FileVersion", "id", fileVersionId);
        }
        repository.deleteById(fileVersionId);
    }

    private FileVersionEntity findFileVersion(String fileVersionId) {
        return repository.findById(fileVersionId)
                .orElseThrow(() -> ResourceNotFoundException.byField("FileVersion", "id", fileVersionId));
    }

    private FileEntity findFile(String fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));
    }

    private FileVersionResponse mapToResponse(FileVersionEntity fileVersion) {
        return new FileVersionResponse(
                fileVersion.getId(),
                fileVersion.getFileVersion(),
                fileVersion.getFileHash(),
                fileVersion.getSizeFile(),
                fileVersion.getFile().getId(),
                fileVersion.getCreatedAt(),
                fileVersion.getUpdatedAt()
        );
    }
}
