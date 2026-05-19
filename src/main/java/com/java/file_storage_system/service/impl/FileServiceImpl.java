package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.MessageConstants;
import com.java.file_storage_system.constant.FileStatus;
import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.dto.file.CreateFileRequest;
import com.java.file_storage_system.dto.file.FileResponse;
import com.java.file_storage_system.dto.file.RecycleBinFileResponse;
import com.java.file_storage_system.dto.file.UpdateFileRequest;
import com.java.file_storage_system.entity.FileEntity;
import com.java.file_storage_system.entity.FolderEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.entity.UserProjectEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FileRepository;
import com.java.file_storage_system.repository.FileShareRepository;
import com.java.file_storage_system.repository.FolderRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.repository.UserProjectRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileServiceImpl extends BaseServiceImpl<FileEntity, FileRepository> implements FileService {

    private final TenantRepository tenantRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final FileShareRepository fileShareRepository;
    private final UserProjectRepository userProjectRepository;
    private final UserContext userContext;
    @Override
    @Transactional
    public FileResponse renameFile(String fileId, String newName) {
        FileEntity file = findFile(fileId);
        file.setNameFile(normalizeRequired(newName, "nameFile"));
        return mapToResponse(repository.save(file));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getAllFiles() {
        // Aspect sẽ check READ permission trước khi method được gọi
        // Nếu user không có quyền → ForbiddenException throw ra
        // Nên ở đây chỉ cần return accessible files
        // Tuy nhiên, hiện tại chưa có cách để biết user vừa được check → return all
        // TODO: Implement sophisticated filtering khi cần (query files by user's accessible folders/projects/fileshare)
        return repository.findAllByDeletedAtIsNull().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getFilesByFolder(String folderId) {
        return repository.findAllByFolder_IdAndDeletedAtIsNull(folderId).stream().map(this::mapToResponse).toList();
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
        FileEntity file = findFile(fileId);
        if (file.getDeletedAt() == null) {
            file.setDeletedAt(LocalDateTime.now());
            file.setStatusFile(FileStatus.DELETED);
            repository.save(file);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecycleBinFileResponse> getRecycleBinFiles() {
        return repository.findAllByTenant_IdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userContext.getTenantId())
                .stream()
                .map(this::mapToRecycleBinResponse)
                .toList();
    }

    @Override
    @Transactional
    public void restoreFile(String fileId) {
        FileEntity file = findFileByIdForCurrentTenant(fileId);
        if (file.getDeletedAt() != null) {
            file.setDeletedAt(null);
            file.setStatusFile(FileStatus.APPROVED);
            repository.save(file);
        }
    }

    @Override
    @Transactional
    public void permanentlyDeleteFile(String fileId) {
        FileEntity file = findFileByIdForCurrentTenant(fileId);
        if (file.getDeletedAt() == null) {
            throw ConflictException.withMessage("File is not in recycle bin");
        }
        repository.delete(file);
    }

    @Override
    @Transactional
    public void emptyRecycleBin() {
        List<FileEntity> deletedFiles = repository.findAllByTenant_IdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userContext.getTenantId());
        if (deletedFiles.isEmpty()) {
            return;
        }
        repository.deleteAllInBatch(deletedFiles);
    }

    private FileEntity findFile(String fileId) {
        return repository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));
    }

    private FileEntity findFileByIdForCurrentTenant(String fileId) {
        FileEntity file = repository.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));

        String currentTenantId = userContext.getTenantId();
        if (!file.getTenant().getId().equals(currentTenantId)) {
            throw new ForbiddenException("You do not have access to this file");
        }

        return file;
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

    private RecycleBinFileResponse mapToRecycleBinResponse(FileEntity file) {
        return new RecycleBinFileResponse(
                file.getId(),
                file.getNameFile(),
                file.getSizeFile(),
                file.getStatusFile(),
                file.getFolder().getId(),
                file.getFolder().getPath(),
                file.getFolder().getProject().getId(),
                file.getDeletedAt(),
                file.getUpdatedAt()
        );
    }

    /**
     * Helper: Lấy effective permission của user cho file.
     * 1. Check FileShare (valid & not expired)
     * 2. Fallback folder ACL
     * 3. Fallback project membership
     * Trả về bitmask permission (1=READ, 2=WRITE, 4=DELETE) hoặc null
     */
    public Integer resolveEffectiveFilePermission(String fileId, String userId) {
        FileEntity file = findFile(fileId);
        FolderEntity folder = file.getFolder();

        // 1. Check FileShare first
        var fileShareOpt = fileShareRepository.findValidFileShare(
            fileId,
            userId,
                LocalDateTime.now()
        );

        if (fileShareOpt.isPresent()) {
            var fileShare = fileShareOpt.get();
            return mapFileSharePermissionToBit(fileShare.getPermission());
        }

        // 2. Fallback to folder ACL
        var folderAclOpt = folderRepository.findFolderAclPermission(folder.getId(), userId);
        if (folderAclOpt.isPresent()) {
            return folderAclOpt.get();
        }

        // 3. Fallback to project membership
        return userProjectRepository.findByUserIdAndProjectId(userId, folder.getProject().getId())
                .map(UserProjectEntity::getPermission)
                .orElse(null);
    }

    /**
     * Map FileSharePermission enum → bitmask bit
     * VIEW (1) + COMMENT (1) → READ
     * EDIT (3) → READ + WRITE
     */
    private int mapFileSharePermissionToBit(com.java.file_storage_system.constant.FileSharePermission permission) {
        return switch (permission) {
            case VIEW -> 1;      // READ only
            case COMMENT -> 1;   // READ only (has comment action)
            case EDIT -> 3;      // READ + WRITE (no DELETE)
        };
    }

    /**
     * Check nếu user có access READ quyền trên file
     */
    public boolean canUserReadFile(String fileId, String userId) {
        try {
            FileEntity file = findFile(fileId);

            // File owner luôn có quyền
            if (file.getOwner().getId().equals(userId)) {
                return true;
            }

            // Project owner luôn có quyền
            if (file.getFolder().getProject().getOwner().getId().equals(userId)) {
                return true;
            }

            // Check effective permission
            Integer permission = resolveEffectiveFilePermission(fileId, userId);
            return permission != null && (permission & 1) != 0;  // 1 = READ bit
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check nếu user có WRITE quyền + file không bị lock bởi user khác
     */
    public boolean canUserEditFile(String fileId, String userId) {
        try {
            FileEntity file = findFile(fileId);

            // File owner có quyền (nếu không bị lock bởi người khác)
            if (file.getOwner().getId().equals(userId)) {
                if (file.getLockedByUser() != null && !file.getLockedByUser().getId().equals(userId)) {
                    return false;
                }
                return true;
            }

            // Project owner có quyền (nếu không bị lock bởi người khác)
            if (file.getFolder().getProject().getOwner().getId().equals(userId)) {
                if (file.getLockedByUser() != null && !file.getLockedByUser().getId().equals(userId)) {
                    return false;
                }
                return true;
            }

            // Check file lock
            if (file.getLockedByUser() != null && !file.getLockedByUser().getId().equals(userId)) {
                return false;
            }

            // Check WRITE permission (bit 2)
            Integer permission = resolveEffectiveFilePermission(fileId, userId);
            return permission != null && (permission & 2) != 0;  // 2 = WRITE bit
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check nếu user có DELETE quyền trên file
     */
    public boolean canUserDeleteFile(String fileId, String userId) {
        try {
            FileEntity file = findFile(fileId);

            // File owner có quyền xoá (nếu không bị lock bởi người khác)
            if (file.getOwner().getId().equals(userId)) {
                if (file.getLockedByUser() != null && !file.getLockedByUser().getId().equals(userId)) {
                    return false;
                }
                return true;
            }

            // Project owner có quyền xoá (nếu không bị lock bởi người khác)
            if (file.getFolder().getProject().getOwner().getId().equals(userId)) {
                if (file.getLockedByUser() != null && !file.getLockedByUser().getId().equals(userId)) {
                    return false;
                }
                return true;
            }

            // Check file lock
            if (file.getLockedByUser() != null && !file.getLockedByUser().getId().equals(userId)) {
                return false;
            }

            // Check DELETE permission (bit 4)
            Integer permission = resolveEffectiveFilePermission(fileId, userId);
            return permission != null && (permission & 4) != 0;  // 4 = DELETE bit
        } catch (Exception e) {
            return false;
        }
    }
}
