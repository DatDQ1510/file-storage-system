package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.MessageConstants;
import com.java.file_storage_system.dto.folder.CreateFolderRequest;
import com.java.file_storage_system.dto.folder.CreateFolderWithAclRequest;
import com.java.file_storage_system.dto.folder.CreateFolderWithAclResponse;
import com.java.file_storage_system.dto.folder.FolderResponse;
import com.java.file_storage_system.dto.folder.FolderAclItemRequest;
import com.java.file_storage_system.dto.folder.FolderAclItemResponse;
import com.java.file_storage_system.dto.folder.FolderPathNodeResponse;
import com.java.file_storage_system.dto.folder.ProjectMemberForAclResponse;
import com.java.file_storage_system.dto.folder.RenameFolderRequest;
import com.java.file_storage_system.dto.folder.UpdateFolderRequest;
import com.java.file_storage_system.dto.folder.UpsertFolderAclRequest;
import com.java.file_storage_system.entity.FolderAclEntity;
import com.java.file_storage_system.entity.FolderEntity;
import com.java.file_storage_system.entity.ProjectEntity;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.entity.UserProjectEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FolderAclRepository;
import com.java.file_storage_system.repository.FolderRepository;
import com.java.file_storage_system.repository.ProjectRepository;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.repository.UserProjectRepository;
import com.java.file_storage_system.service.FolderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FolderServiceImpl extends BaseServiceImpl<FolderEntity, FolderRepository> implements FolderService {

    private static final int PERMISSION_READ = 1;
    private static final int PERMISSION_WRITE = 2;
    private static final int PERMISSION_DELETE = 4;
    private static final int PERMISSION_FOLDER_MAX = PERMISSION_READ | PERMISSION_WRITE | PERMISSION_DELETE; // 7

    private final TenantRepository tenantRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserProjectRepository userProjectRepository;
    private final FolderAclRepository folderAclRepository;

    public FolderServiceImpl(
            FolderRepository repository,
            TenantRepository tenantRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            UserProjectRepository userProjectRepository,
            FolderAclRepository folderAclRepository) {
        super(repository);
        this.tenantRepository = tenantRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userProjectRepository = userProjectRepository;
        this.folderAclRepository = folderAclRepository;
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

    @Override
    public List<FolderResponse> getFoldersByProject(
            String projectId,
            String actorId,
            String actorRole,
            String actorTenantId) {
        ProjectEntity project = findProject(projectId);
        validateActorCanAccessProject(project, actorId, actorRole, actorTenantId);

        return repository.findAllByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public CreateFolderWithAclResponse createFolderWithAcl(
            String projectId,
            CreateFolderWithAclRequest request,
            String actorId,
            String actorRole,
            String actorTenantId) {
        ProjectEntity project = findProject(projectId);
        validateActorCanCreateFolder(project, actorId, actorRole, actorTenantId);

        TenantEntity tenant = project.getTenant();
        UserEntity owner = resolveFolderOwner(project, actorId, actorRole);

        String parentPath = normalizeBasePath(request.path());
        FolderEntity parent = resolveParentFolder(project, request.parentFolderId(), parentPath);
        validateParentConsistency(project, tenant, parent);

        String folderName = normalizeFolderName(request.nameFolder());
        String finalPath = buildFolderPath(parentPath, folderName);
        if (repository.existsByProjectIdAndPath(projectId, finalPath)) {
            throw new ConflictException("Folder path already exists in project: " + finalPath);
        }

        FolderEntity folder = new FolderEntity();
        folder.setNameFolder(folderName);
        folder.setPath(finalPath);
        folder.setTenant(tenant);
        folder.setProject(project);
        folder.setOwner(owner);
        folder.setParentFolder(parent);
        FolderEntity savedFolder = repository.save(folder);

        List<FolderAclEntity> aclEntries = createFolderAclEntries(savedFolder, request.aclEntries(), project);

        return new CreateFolderWithAclResponse(
                mapToResponse(savedFolder),
                aclEntries.stream().map(this::mapToAclItemResponse).toList());
    }

    @Override
    public List<FolderPathNodeResponse> getChildFolderPaths(
            String projectId,
            String parentPath,
            String actorId,
            String actorRole,
            String actorTenantId) {
        ProjectEntity project = findProject(projectId);
        validateActorCanAccessProject(project, actorId, actorRole, actorTenantId);

        String normalizedParentPath = normalizeBasePath(parentPath);
        List<FolderEntity> projectFolders = repository.findAllByProjectId(projectId);
        Set<String> parentPathsWithChildren = buildParentPathSet(projectFolders);

        return projectFolders.stream()
                .filter(folder -> isDirectChildPath(folder.getPath(), normalizedParentPath))
                .sorted(Comparator.comparing(folder -> folder.getPath().toLowerCase()))
                .map(folder -> mapToFolderPathNode(folder, parentPathsWithChildren))
                .toList();
    }

    @Override
    public List<FolderPathNodeResponse> searchFolderPaths(
            String projectId,
            String keyword,
            String actorId,
            String actorRole,
            String actorTenantId) {
        ProjectEntity project = findProject(projectId);
        validateActorCanAccessProject(project, actorId, actorRole, actorTenantId);

        String normalizedKeyword = normalizePathKeyword(keyword);
        if (normalizedKeyword == null) {
            return List.of();
        }

        Set<String> parentPathsWithChildren = buildParentPathSet(repository.findAllByProjectId(projectId));

        return repository.searchByProjectIdAndPathKeyword(projectId, normalizedKeyword)
                .stream()
                .map(folder -> mapToFolderPathNode(folder, parentPathsWithChildren))
                .toList();
    }

    @Override
    public List<ProjectMemberForAclResponse> getProjectMembersForAcl(
            String projectId,
            String actorId,
            String actorRole,
            String actorTenantId) {
        ProjectEntity project = findProject(projectId);
        validateActorCanAccessProject(project, actorId, actorRole, actorTenantId);

        // Collect all members; use a map keyed by userId to deduplicate
        Map<String, ProjectMemberForAclResponse> membersMap = new LinkedHashMap<>();

        // Include owner first
        UserEntity owner = project.getOwner();
        membersMap.put(owner.getId(), new ProjectMemberForAclResponse(
                owner.getId(),
                owner.getUserName(),
                owner.getEmail()));

        // Include all UserProject members
        userProjectRepository.findAllByProjectId(projectId).forEach(up -> {
            UserEntity user = up.getUser();
            membersMap.putIfAbsent(user.getId(), new ProjectMemberForAclResponse(
                    user.getId(),
                    user.getUserName(),
                    user.getEmail()));
        });

        return new ArrayList<>(membersMap.values());
    }

    @Override
    public List<FolderAclItemResponse> getFolderAcl(
            String folderId,
            String actorId,
            String actorRole,
            String actorTenantId) {
        FolderEntity folder = repository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));
        validateActorCanAccessProject(folder.getProject(), actorId, actorRole, actorTenantId);

        return folderAclRepository.findAllByFolderId(folderId)
                .stream()
                .map(this::mapToAclItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public FolderAclItemResponse upsertFolderAcl(
            String folderId,
            String userId,
            UpsertFolderAclRequest request,
            String actorId,
            String actorRole,
            String actorTenantId) {
        FolderEntity folder = repository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));
        ProjectEntity project = folder.getProject();
        validateActorCanCreateFolder(project, actorId, actorRole, actorTenantId);

        UserEntity targetUser = findUser(userId);
        if (!project.getTenant().getId().equals(targetUser.getTenant().getId())) {
            throw new ForbiddenException("ACL user must belong to the same tenant as project");
        }
        boolean isOwner = project.getOwner().getId().equals(userId);
        boolean isMember = userProjectRepository.existsByUserIdAndProjectId(userId, project.getId());
        if (!isOwner && !isMember) {
            throw new ForbiddenException("ACL user must be project owner or member");
        }

        int permission = normalizeFolderPermission(request.permission());

        FolderAclEntity acl = folderAclRepository.findByFolderIdAndUserId(folderId, userId)
                .orElseGet(() -> {
                    FolderAclEntity newAcl = new FolderAclEntity();
                    newAcl.setFolder(folder);
                    newAcl.setUser(targetUser);
                    return newAcl;
                });
        acl.setPermission(permission);
        FolderAclEntity saved = folderAclRepository.save(acl);
        return mapToAclItemResponse(saved);
    }

    @Override
    @Transactional
    public FolderResponse renameFolder(
            String folderId,
            RenameFolderRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
    ) {
        FolderEntity folder = repository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));

        validateActorCanAccessProject(folder.getProject(), actorId, actorRole, actorTenantId);

        // Tính toán path mới dựa trên parent path + tên mới
        String newName = normalizeFolderName(request.nameFolder());
        String currentPath = folder.getPath();
        String parentPath = deriveParentPath(currentPath);
        String newPath = buildFolderPath(parentPath, newName);

        // Kiểm tra path mới không trùng (trừ chính folder hiện tại)
        if (!newPath.equals(currentPath)
                && repository.existsByProjectIdAndPath(folder.getProject().getId(), newPath)) {
            throw new ConflictException("Folder path already exists in project: " + newPath);
        }

        folder.setNameFolder(newName);
        folder.setPath(newPath);
        return mapToResponse(repository.save(folder));
    }

    @Override
    @Transactional
    public void deleteFolderByActor(
            String folderId,
            String actorId,
            String actorRole,
            String actorTenantId
    ) {
        FolderEntity folder = repository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));

        validateActorCanAccessProject(folder.getProject(), actorId, actorRole, actorTenantId);

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

    private String normalizePathKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeBasePath(String path) {
        String normalizedPath = normalizePath(path);
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        while (normalizedPath.contains("//")) {
            normalizedPath = normalizedPath.replace("//", "/");
        }

        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        return normalizedPath;
    }

    private String normalizeFolderName(String folderName) {
        String normalizedFolderName = normalizeRequired(folderName, "nameFolder");
        if (normalizedFolderName.contains("/")) {
            throw ConflictException.withMessage("nameFolder must not contain '/'");
        }
        return normalizedFolderName;
    }

    private FolderEntity resolveParentFolder(ProjectEntity project, String parentFolderId, String parentPath) {
        if (parentFolderId != null && !parentFolderId.isBlank()) {
            return findParentFolderOrNull(parentFolderId);
        }

        if ("/".equals(parentPath)) {
            return null;
        }

        return repository.findByProjectIdAndPath(project.getId(), parentPath)
                .orElseThrow(() -> new ResourceNotFoundException("Parent path not found: " + parentPath));
    }

    private String buildFolderPath(String parentPath, String folderName) {
        if ("/".equals(parentPath)) {
            return "/" + folderName;
        }
        return parentPath + "/" + folderName;
    }

    private boolean isDirectChildPath(String fullPath, String parentPath) {
        String normalizedFullPath = normalizeBasePath(fullPath);
        String normalizedParentPath = normalizeBasePath(parentPath);

        if ("/".equals(normalizedParentPath)) {
            if (!normalizedFullPath.startsWith("/") || normalizedFullPath.equals("/")) {
                return false;
            }
            String remainder = normalizedFullPath.substring(1);
            return !remainder.isEmpty() && !remainder.contains("/");
        }

        String prefix = normalizedParentPath + "/";
        if (!normalizedFullPath.startsWith(prefix)) {
            return false;
        }

        String remainder = normalizedFullPath.substring(prefix.length());
        return !remainder.isEmpty() && !remainder.contains("/");
    }

    private void validateActorCanAccessProject(
            ProjectEntity project,
            String actorId,
            String actorRole,
            String actorTenantId) {
        if ("TENANT_ADMIN".equals(actorRole)) {
            if (actorTenantId == null || !actorTenantId.equals(project.getTenant().getId())) {
                throw new ForbiddenException("TenantAdmin không cùng tenant với project");
            }
            return;
        }

        if ("USER".equals(actorRole)) {
            if (actorTenantId == null || !actorTenantId.equals(project.getTenant().getId())) {
                throw new ForbiddenException("User không cùng tenant với project");
            }

            boolean isOwner = project.getOwner().getId().equals(actorId);
            boolean isMember = userProjectRepository.existsByUserIdAndProjectId(actorId, project.getId());

            if (!isOwner && !isMember) {
                throw new ForbiddenException("User does not have access to this project");
            }
            return;
        }

        throw new ForbiddenException("Role không được phép truy cập project");
    }

    private void validateActorCanCreateFolder(
            ProjectEntity project,
            String actorId,
            String actorRole,
            String actorTenantId) {
        validateActorCanAccessProject(project, actorId, actorRole, actorTenantId);

        if ("TENANT_ADMIN".equals(actorRole)) {
            return;
        }

        if (project.getOwner().getId().equals(actorId)) {
            return;
        }

        UserProjectEntity membership = userProjectRepository.findByUserIdAndProjectId(actorId, project.getId())
                .orElseThrow(() -> new ForbiddenException("User does not have access to this project"));

        if (!hasPermission(membership.getPermission(), PERMISSION_WRITE)) {
            throw new ForbiddenException("User không có quyền tạo folder trong project");
        }
    }

    private UserEntity resolveFolderOwner(ProjectEntity project, String actorId, String actorRole) {
        if ("USER".equals(actorRole)) {
            return findUser(actorId);
        }
        return project.getOwner();
    }

    private List<FolderAclEntity> createFolderAclEntries(
            FolderEntity folder,
            List<FolderAclItemRequest> aclRequests,
            ProjectEntity project) {
        if (aclRequests == null || aclRequests.isEmpty()) {
            return List.of();
        }

        Set<String> userIds = new HashSet<>();
        List<FolderAclEntity> aclEntities = new ArrayList<>();

        for (FolderAclItemRequest aclRequest : aclRequests) {
            String userId = normalizeRequired(aclRequest.userId(), "userId");
            if (!userIds.add(userId)) {
                throw ConflictException.withMessage("Duplicate ACL userId: " + userId);
            }

            UserEntity user = findUser(userId);
            if (!project.getTenant().getId().equals(user.getTenant().getId())) {
                throw new ForbiddenException("ACL user must belong to the same tenant as project");
            }

            boolean isProjectOwner = project.getOwner().getId().equals(userId);
            boolean isProjectMember = userProjectRepository.existsByUserIdAndProjectId(userId, project.getId());
            if (!isProjectOwner && !isProjectMember) {
                throw new ForbiddenException("ACL user must be project owner or member");
            }

            FolderAclEntity acl = new FolderAclEntity();
            acl.setFolder(folder);
            acl.setUser(user);
            acl.setPermission(normalizeFolderPermission(aclRequest.permission()));
            aclEntities.add(folderAclRepository.save(acl));
        }

        return aclEntities;
    }

    private boolean hasPermission(Integer permission, int expectedPermission) {
        if (permission == null) {
            return false;
        }
        return (permission & expectedPermission) == expectedPermission;
    }

    /**
     * Normalize a folder ACL permission value to the valid bitmask range [1, 7].
     * Defaults to PERMISSION_READ (1) if null or out of range.
     */
    private int normalizeFolderPermission(Integer permission) {
        if (permission == null || permission < 1 || permission > PERMISSION_FOLDER_MAX) {
            return PERMISSION_READ;
        }
        return permission;
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
                folder.getUpdatedAt());
    }

    private FolderAclItemResponse mapToAclItemResponse(FolderAclEntity acl) {
        return new FolderAclItemResponse(
                acl.getId(),
                acl.getUser().getId(),
                acl.getUser().getUserName(),
                acl.getPermission());
    }

    private FolderPathNodeResponse mapToFolderPathNode(FolderEntity folder, Set<String> parentPathsWithChildren) {
        boolean hasChildren = parentPathsWithChildren.contains(folder.getPath());
        return new FolderPathNodeResponse(
                folder.getId(),
                folder.getNameFolder(),
                folder.getPath(),
                hasChildren);
    }

    private Set<String> buildParentPathSet(List<FolderEntity> folders) {
        Set<String> parentPaths = new HashSet<>();
        for (FolderEntity folder : folders) {
            parentPaths.add(deriveParentPath(folder.getPath()));
        }
        return parentPaths;
    }

    private String deriveParentPath(String fullPath) {
        String normalizedPath = normalizeBasePath(fullPath);
        if ("/".equals(normalizedPath)) {
            return "/";
        }

        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        if (lastSlashIndex <= 0) {
            return "/";
        }

        return normalizedPath.substring(0, lastSlashIndex);
    }
}
