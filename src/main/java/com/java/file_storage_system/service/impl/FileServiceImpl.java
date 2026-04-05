package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.MessageConstants;
import com.java.file_storage_system.dto.file.CreateFileRequest;
import com.java.file_storage_system.dto.file.FileResponse;
import com.java.file_storage_system.dto.file.UpdateFileRequest;
import com.java.file_storage_system.entity.FileEntity;
import com.java.file_storage_system.entity.FolderEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FileRepository;
import com.java.file_storage_system.repository.FolderRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileServiceImpl extends BaseServiceImpl<FileEntity, FileRepository> implements FileService {

    private final TenantRepository tenantRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getAllFiles() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(String fileId) {
        return mapToResponse(findFile(fileId));
    }

    @Override
    @Transactional
    public FileResponse createFile(CreateFileRequest request) {
        TenantEntity tenant = findTenant(request.tenantId());
        FolderEntity folder = findFolder(request.folderId());
        UserEntity owner = findUser(request.ownerId());
        UserEntity lockedByUser = findOptionalUser(request.lockedByUserId());

        validateTenantConsistency(tenant, folder, owner, lockedByUser);

        FileEntity file = new FileEntity();
        file.setNameFile(normalizeRequired(request.nameFile(), "nameFile"));
        file.setStatusFile(request.statusFile());
        file.setSizeFile(request.sizeFile());
        file.setExtraInfo(request.extraInfo());
        file.setTenant(tenant);
        file.setFolder(folder);
        file.setOwner(owner);
        file.setLockedByUser(lockedByUser);

        return mapToResponse(repository.save(file));
    }

    @Override
    @Transactional
    public FileResponse updateFile(String fileId, UpdateFileRequest request) {
        FileEntity file = findFile(fileId);

        TenantEntity tenant = findTenant(request.tenantId());
        FolderEntity folder = findFolder(request.folderId());
        UserEntity owner = findUser(request.ownerId());
        UserEntity lockedByUser = findOptionalUser(request.lockedByUserId());

        validateTenantConsistency(tenant, folder, owner, lockedByUser);

        file.setNameFile(normalizeRequired(request.nameFile(), "nameFile"));
        file.setStatusFile(request.statusFile());
        file.setSizeFile(request.sizeFile());
        file.setExtraInfo(request.extraInfo());
        file.setTenant(tenant);
        file.setFolder(folder);
        file.setOwner(owner);
        file.setLockedByUser(lockedByUser);

        return mapToResponse(repository.save(file));
    }

    @Override
    @Transactional
    public void deleteFile(String fileId) {
        if (!repository.existsById(fileId)) {
            throw ResourceNotFoundException.byField("File", "id", fileId);
        }
        repository.deleteById(fileId);
    }

    private FileEntity findFile(String fileId) {
        return repository.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));
    }

    private TenantEntity findTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Tenant", "id", tenantId));
    }

    private FolderEntity findFolder(String folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.byField("User", "id", userId));
    }

    private UserEntity findOptionalUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return findUser(userId.trim());
    }

    private void validateTenantConsistency(TenantEntity tenant, FolderEntity folder, UserEntity owner, UserEntity lockedByUser) {
        if (!tenant.getId().equals(folder.getTenant().getId())) {
            throw new ForbiddenException(MessageConstants.FOLDER_TENANT_MISMATCH);
        }

        if (!tenant.getId().equals(owner.getTenant().getId())) {
            throw new ForbiddenException(MessageConstants.TENANT_OWNER_MISMATCH);
        }

        if (lockedByUser != null && !tenant.getId().equals(lockedByUser.getTenant().getId())) {
            throw new ForbiddenException("Locked user does not belong to the selected tenant");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw ConflictException.withMessage(fieldName + " is required");
        }
        return value.trim();
    }

    private FileResponse mapToResponse(FileEntity file) {
        return new FileResponse(
                file.getId(),
                file.getNameFile(),
                file.getStatusFile(),
                file.getSizeFile(),
                file.getExtraInfo(),
                file.getTenant().getId(),
                file.getFolder().getId(),
                file.getOwner().getId(),
                file.getLockedByUser() == null ? null : file.getLockedByUser().getId(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }
}
