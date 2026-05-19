package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.FileStatus;
import com.java.file_storage_system.dto.starred.StarredFileResponse;
import com.java.file_storage_system.dto.starred.StarredFolderResponse;
import com.java.file_storage_system.dto.starred.StarredPageResponse;
import com.java.file_storage_system.entity.FileEntity;
import com.java.file_storage_system.entity.FileStarEntity;
import com.java.file_storage_system.entity.FolderEntity;
import com.java.file_storage_system.entity.FolderStarEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FileRepository;
import com.java.file_storage_system.repository.FileStarRepository;
import com.java.file_storage_system.repository.FolderRepository;
import com.java.file_storage_system.repository.FolderStarRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.StarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class StarServiceImpl implements StarService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final FileStarRepository fileStarRepository;
    private final FolderStarRepository folderStarRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public StarredPageResponse getStarredPage(String userId, String tenantId) {
        List<StarredFolderResponse> folders = folderStarRepository
                .findAllByUserAndTenantOrderByCreatedAtDesc(userId, tenantId)
                .stream()
                .map(this::mapToFolderResponse)
                .toList();

        List<StarredFileResponse> files = fileStarRepository
                .findAllByUserAndTenantOrderByCreatedAtDesc(userId, tenantId)
                .stream()
                .map(this::mapToFileResponse)
                .toList();

        return new StarredPageResponse(folders, files);
    }

    @Override
    @Transactional
    public StarredFileResponse starFile(String fileId, String userId, String tenantId) {
        FileEntity file = findFile(fileId);
        UserEntity user = findUser(userId);
        TenantEntity tenant = findTenant(tenantId);

        ensureTenantConsistency(file.getTenant().getId(), tenantId, "File");

        FileStarEntity star = fileStarRepository.findByFile_IdAndUser_Id(fileId, userId)
                .orElseGet(FileStarEntity::new);
        star.setFile(file);
        star.setUser(user);
        star.setTenant(tenant);

        return mapToFileResponse(fileStarRepository.save(star));
    }

    @Override
    @Transactional
    public void unstarFile(String fileId, String userId) {
        fileStarRepository.deleteByFile_IdAndUser_Id(fileId, userId);
    }

    @Override
    @Transactional
    public StarredFolderResponse starFolder(String folderId, String userId, String tenantId) {
        FolderEntity folder = findFolder(folderId);
        UserEntity user = findUser(userId);
        TenantEntity tenant = findTenant(tenantId);

        ensureTenantConsistency(folder.getTenant().getId(), tenantId, "Folder");

        FolderStarEntity star = folderStarRepository.findByFolder_IdAndUser_Id(folderId, userId)
                .orElseGet(FolderStarEntity::new);
        star.setFolder(folder);
        star.setUser(user);
        star.setTenant(tenant);

        return mapToFolderResponse(folderStarRepository.save(star));
    }

    @Override
    @Transactional
    public void unstarFolder(String folderId, String userId) {
        folderStarRepository.deleteByFolder_IdAndUser_Id(folderId, userId);
    }

    private StarredFileResponse mapToFileResponse(FileStarEntity star) {
        FileEntity file = star.getFile();
        FolderEntity folder = file.getFolder();
        return new StarredFileResponse(
                star.getId(),
                file.getId(),
                file.getNameFile(),
                folder.getId(),
                folder.getPath(),
                folder.getProject().getId(),
                file.getSizeFile(),
                file.getStatusFile() == null ? null : file.getStatusFile().name(),
                star.getCreatedAt(),
                file.getUpdatedAt()
        );
    }

    private StarredFolderResponse mapToFolderResponse(FolderStarEntity star) {
        FolderEntity folder = star.getFolder();
        return new StarredFolderResponse(
                star.getId(),
                folder.getId(),
                folder.getNameFolder(),
                folder.getPath(),
                folder.getProject().getId(),
                star.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }

    private FileEntity findFile(String fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));
    }

    private FolderEntity findFolder(String folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));
    }

    private TenantEntity findTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Tenant", "id", tenantId));
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.byField("User", "id", userId));
    }

    private void ensureTenantConsistency(String resourceTenantId, String tenantId, String resourceName) {
        if (!resourceTenantId.equals(tenantId)) {
            throw new ResourceNotFoundException(resourceName + " not found in tenant " + tenantId);
        }
    }
}