package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.MessageConstants;
import com.java.file_storage_system.dto.folder.CreateFolderRequest;
import com.java.file_storage_system.dto.folder.FolderResponse;
import com.java.file_storage_system.dto.folder.UpdateFolderRequest;
import com.java.file_storage_system.entity.FolderEntity;
import com.java.file_storage_system.entity.ProjectEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FolderRepository;
import com.java.file_storage_system.repository.ProjectRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.FolderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FolderServiceImpl extends BaseServiceImpl<FolderEntity, FolderRepository> implements FolderService {

    private final TenantRepository tenantRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public FolderServiceImpl(
            FolderRepository repository,
            TenantRepository tenantRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        super(repository);
        this.tenantRepository = tenantRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<FolderResponse> getAllFolders() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public FolderResponse getFolderById(String folderId) {
        FolderEntity folder = repository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));
        return mapToResponse(folder);
    }

    @Override
    public FolderResponse createFolder(CreateFolderRequest request) {
        TenantEntity tenant = findTenant(request.tenantId());
        ProjectEntity project = findProject(request.projectId());
        UserEntity owner = findUser(request.ownerId());

        validateTenantConsistency(tenant, project, owner);

        FolderEntity parent = findParentFolderOrNull(request.parentFolderId());
        validateParentConsistency(project, tenant, parent);

        FolderEntity folder = new FolderEntity();
        folder.setNameFolder(normalizeRequired(request.nameFolder(), "nameFolder"));
        folder.setPath(normalizePath(request.path()));
        folder.setTenant(tenant);
        folder.setProject(project);
        folder.setOwner(owner);
        folder.setParentFolder(parent);

        return mapToResponse(repository.save(folder));
    }

    @Override
    public FolderResponse updateFolder(String folderId, UpdateFolderRequest request) {
        FolderEntity folder = repository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));

        UserEntity owner = findUser(request.ownerId());
        if (!owner.getTenant().getId().equals(folder.getTenant().getId())) {
            throw new ForbiddenException(MessageConstants.FOLDER_OWNER_NOT_IN_TENANT);
        }

        FolderEntity parent = findParentFolderOrNull(request.parentFolderId());
        validateParentConsistency(folder.getProject(), folder.getTenant(), parent);
        if (parent != null && folder.getId().equals(parent.getId())) {
            throw new ForbiddenException(MessageConstants.FOLDER_CANNOT_BE_OWN_PARENT);
        }

        folder.setNameFolder(normalizeRequired(request.nameFolder(), "nameFolder"));
        folder.setPath(normalizePath(request.path()));
        folder.setOwner(owner);
        folder.setParentFolder(parent);

        return mapToResponse(repository.save(folder));
    }

    @Override
    public void deleteFolder(String folderId) {
        FolderEntity folder = repository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));
        repository.delete(folder);
    }

    private TenantEntity findTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Tenant", "id", tenantId));
    }

    private ProjectEntity findProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Project", "id", projectId));
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.byField("User", "id", userId));
    }

    private FolderEntity findParentFolderOrNull(String parentFolderId) {
        if (parentFolderId == null || parentFolderId.isBlank()) {
            return null;
        }

        return repository.findById(parentFolderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", parentFolderId));
    }

    private void validateTenantConsistency(TenantEntity tenant, ProjectEntity project, UserEntity owner) {
        if (!project.getTenant().getId().equals(tenant.getId())) {
            throw new ForbiddenException(MessageConstants.TENANT_PROJECT_MISMATCH);
        }
        if (!owner.getTenant().getId().equals(tenant.getId())) {
            throw new ForbiddenException(MessageConstants.TENANT_OWNER_MISMATCH);
        }
    }

    private void validateParentConsistency(ProjectEntity project, TenantEntity tenant, FolderEntity parent) {
        if (parent == null) {
            return;
        }

        if (!parent.getProject().getId().equals(project.getId())) {
            throw new ForbiddenException(MessageConstants.FOLDER_PROJECT_MISMATCH);
        }
        if (!parent.getTenant().getId().equals(tenant.getId())) {
            throw new ForbiddenException(MessageConstants.FOLDER_TENANT_MISMATCH);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw ConflictException.withMessage(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        return path.trim();
    }

    private FolderResponse mapToResponse(FolderEntity folder) {
        return new FolderResponse(
                folder.getId(),
                folder.getNameFolder(),
                folder.getPath(),
                folder.getTenant().getId(),
                folder.getProject().getId(),
                folder.getOwner().getId(),
                folder.getParentFolder() == null ? null : folder.getParentFolder().getId(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }
}
