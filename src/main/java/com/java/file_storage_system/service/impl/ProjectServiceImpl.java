package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.MessageConstants;
import com.java.file_storage_system.constant.ProjectStatus;
import com.java.file_storage_system.dto.project.ProjectPageResponse;
import com.java.file_storage_system.dto.project.ProjectRequest;
import com.java.file_storage_system.dto.project.ProjectResponse;
import com.java.file_storage_system.dto.project.member.AddProjectMemberRequest;
import com.java.file_storage_system.dto.project.member.AssignProjectMemberRequest;
import com.java.file_storage_system.dto.project.member.ProjectMemberResponse;
import com.java.file_storage_system.entity.ProjectEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.entity.UserProjectEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.ProjectRepository;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.repository.UserProjectRepository;
import com.java.file_storage_system.repository.UserRepository;
import com.java.file_storage_system.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends BaseServiceImpl<ProjectEntity, ProjectRepository> implements ProjectService {

    private static final int PERMISSION_READ = 1;
    private static final int PERMISSION_WRITE = 2;
    private static final int PERMISSION_DELETE = 4;
    private static final int PERMISSION_MANAGE_MEMBER = 8;
    private static final int PERMISSION_FULL = PERMISSION_READ | PERMISSION_WRITE | PERMISSION_DELETE | PERMISSION_MANAGE_MEMBER;

    private final UserRepository userRepository;
    private final TenantAdminRepository tenantAdminRepository;
    private final UserProjectRepository userProjectRepository;

    @Override
    public ProjectResponse createProject(ProjectRequest request, String tenantAdminId) {
        log.info("Creating project: {} by TenantAdmin: {}", request.nameProject(), tenantAdminId);

        // 1. Kiểm tra TenantAdmin tồn tại
        TenantAdminEntity tenantAdmin = getTenantAdmin(tenantAdminId);

        // 2. Kiểm tra Owner tồn tại
        String ownerId = request.ownerId();
        UserEntity owner = getOwner(ownerId);

        // 3. Kiểm tra Owner có phải thành viên của tenant của TenantAdmin không
        if (!owner.getTenant().getId().equals(tenantAdmin.getTenant().getId())) {
            log.warn("Owner {} không phải thành viên của tenant {}", request.ownerId(), tenantAdmin.getTenant().getId());
            throw new ForbiddenException(MessageConstants.PROJECT_OWNER_NOT_MEMBER);
        }

        // 4. Kiểm tra project name có trùng không trong tenant
        if (repository.existsByNameProjectAndTenantId(request.nameProject(), tenantAdmin.getTenant().getId())) {
            log.warn("Project name {} đã tồn tại trong tenant {}", request.nameProject(), tenantAdmin.getTenant().getId());
            throw new ConflictException(MessageConstants.PROJECT_ALREADY_EXISTS);
        }

        // 5. Tạo project
        ProjectEntity project = new ProjectEntity();
        project.setNameProject(request.nameProject());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setOwner(owner);
        project.setTenant(tenantAdmin.getTenant());
        project.setTenantAdmin(tenantAdmin);

        ProjectEntity savedProject = repository.save(project);
        createProjectMembership(savedProject, owner, PERMISSION_FULL, owner);
        log.info("Project created successfully with ID: {}", savedProject.getId());

        return mapToResponse(savedProject);
    }

    @Override
    public ProjectMemberResponse addUserToProject(
            String projectId,
            AddProjectMemberRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
    ) {
        log.info("addUserToProject called: projectId={}, actorId={}, actorRole={}, targetUserId={}",
            projectId, actorId, actorRole, request.userId());

        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateActorCanManageMembership(project, actorId, actorRole, actorTenantId);

        UserEntity userToAdd = getOwner(request.userId());
        validateUserSameTenant(project, userToAdd);

        if (userProjectRepository.existsByUserIdAndProjectId(userToAdd.getId(), project.getId())) {
            throw new ConflictException(MessageConstants.PROJECT_USER_ALREADY_MEMBER);
        }

        int permission = normalizePermission(request.permission());
        UserEntity grantedBy = resolveGrantedByUser(actorId, actorRole);

        UserProjectEntity membership = createProjectMembership(project, userToAdd, permission, grantedBy);
        return mapToProjectMemberResponse(membership);
    }

    @Override
    public ProjectMemberResponse assignMemberToProject(
            String projectId,
            AssignProjectMemberRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
    ) {
        log.info("assignMemberToProject called: projectId={}, actorId={}, actorRole={}, memberUserId={}",
            projectId, actorId, actorRole, request.memberUserId());

        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateActorCanManageMembership(project, actorId, actorRole, actorTenantId);

        UserEntity userToAdd = getOwner(request.memberUserId());
        validateUserSameTenant(project, userToAdd);

        if (userProjectRepository.existsByUserIdAndProjectId(userToAdd.getId(), project.getId())) {
            throw new ConflictException(MessageConstants.PROJECT_USER_ALREADY_MEMBER);
        }

        int permission = normalizePermission(request.permission());
        UserEntity grantedBy = resolveGrantedByUser(actorId, actorRole);

        UserProjectEntity membership = createProjectMembership(project, userToAdd, permission, grantedBy);
        return mapToProjectMemberResponse(membership);
    }

    @Override
    public ProjectResponse getProjectById(String projectId, String currentUserId, String currentUserRole, String currentTenantId) {
        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateProjectAccess(project, currentUserId, currentUserRole, currentTenantId);
        return mapToResponse(project, currentUserId, currentUserRole, currentTenantId);
    }

    @Override
    public ProjectPageResponse getAllProjectsByTenantAdmin(String tenantAdminId, int page, int size) {
        getTenantAdmin(tenantAdminId);

        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        Page<ProjectEntity> projectPage = repository.findAllByTenantAdminId(
                tenantAdminId,
                PageRequest.of(normalizedPage, normalizedSize)
        );

        return mapToPageResponse(projectPage);
    }

    @Override
    public ProjectPageResponse getAllProjectsByUser(String userId, int page, int size) {
        getOwner(userId);

        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        Page<ProjectEntity> projectPage = repository.findAllByOwnerIdOrMemberUserId(
                userId,
                ProjectStatus.ACTIVE,
                PageRequest.of(normalizedPage, normalizedSize)
        );

        return mapToPageResponse(projectPage);
    }

    @Override
    public ProjectPageResponse searchProjectsByTenantAdmin(String tenantAdminId, String keyword, int page, int size) {
        getTenantAdmin(tenantAdminId);

        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        String normalizedKeyword = normalizeKeyword(keyword);
        log.info("normalizedKeyword",normalizedKeyword) ;
        if (normalizedKeyword == null) {
            Page<ProjectEntity> projectPage = repository.findAllByTenantAdminId(
                    tenantAdminId,
                    PageRequest.of(normalizedPage, normalizedSize)
            );
            return mapToPageResponse(projectPage);
        }

        Page<ProjectEntity> projectPage = repository.searchByTenantAdminIdAndKeyword(
                tenantAdminId,
                normalizedKeyword,
                PageRequest.of(normalizedPage, normalizedSize)
        );

        return mapToPageResponse(projectPage);
    }

    private TenantAdminEntity getTenantAdmin(String tenantAdminId) {
        return tenantAdminRepository.findById(tenantAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantAdmin không tồn tại"));
    }

    private UserEntity getOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            log.warn("getOwner called with blank ownerId");
            throw new ResourceNotFoundException("User không tồn tại");
        }

        String normalizedOwnerId = ownerId.trim();
        return userRepository.findById(normalizedOwnerId)
                .orElseThrow(() -> {
                    log.warn("User not found by id: {}", normalizedOwnerId);
                    return new ResourceNotFoundException("User không tồn tại");
                });
    }


        private ProjectResponse mapToResponse(ProjectEntity project) {
        return ProjectResponse.builder()
            .id(project.getId())
            .nameProject(project.getNameProject())
            .ownerId(project.getOwner().getId())
            .ownerName(project.getOwner().getUserName())
            .updatedAt(project.getUpdatedAt())
            .build();
        }

        private ProjectResponse mapToResponse(
            ProjectEntity project,
            String currentUserId,
            String currentUserRole,
            String currentTenantId
    ) {
        boolean currentUserIsOwner = currentUserId != null && currentUserId.equals(project.getOwner().getId());
        boolean currentUserCanManageMembers = canManageProjectMembers(project, currentUserId, currentUserRole, currentTenantId);

        return ProjectResponse.builder()
                .id(project.getId())
                .nameProject(project.getNameProject())
                .ownerId(project.getOwner().getId())
                .ownerName(project.getOwner().getUserName())
                .currentUserIsOwner(currentUserIsOwner)
                .currentUserCanManageMembers(currentUserCanManageMembers)
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private void validateProjectAccess(
            ProjectEntity project,
            String currentUserId,
            String currentUserRole,
            String currentTenantId
    ) {
        if (currentUserRole == null) {
            throw new ForbiddenException("User role is required");
        }

        if ("TENANT_ADMIN".equals(currentUserRole)) {
            if (currentTenantId == null || !currentTenantId.equals(project.getTenant().getId())) {
                throw new ForbiddenException("TenantAdmin không cùng tenant với project");
            }
            return;
        }

        if ("USER".equals(currentUserRole)) {
            boolean isOwner = currentUserId != null && currentUserId.equals(project.getOwner().getId());
            boolean isMember = currentUserId != null && userProjectRepository.existsByUserIdAndProjectId(currentUserId, project.getId());

            if (!isOwner && !isMember) {
                throw new ForbiddenException("User does not have access to this project");
            }

            if (currentTenantId != null && !currentTenantId.equals(project.getTenant().getId())) {
                throw new ForbiddenException("User không cùng tenant với project");
            }

            return;
        }

        throw new ForbiddenException("Role không được phép truy cập project");
    }

    private boolean canManageProjectMembers(
            ProjectEntity project,
            String currentUserId,
            String currentUserRole,
            String currentTenantId
    ) {
        if (currentUserRole == null) {
            return false;
        }

        if ("TENANT_ADMIN".equals(currentUserRole)) {
            return currentTenantId != null && currentTenantId.equals(project.getTenant().getId());
        }

        return "USER".equals(currentUserRole)
                && currentUserId != null
                && currentUserId.equals(project.getOwner().getId())
                && (currentTenantId == null || currentTenantId.equals(project.getTenant().getId()));
    }

    private ProjectPageResponse mapToPageResponse(Page<ProjectEntity> projectPage) {
        List<ProjectResponse> items = projectPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new ProjectPageResponse(
                items,
                projectPage.getNumber(),
                projectPage.getSize(),
                projectPage.getTotalElements(),
                projectPage.getTotalPages(),
                projectPage.hasNext(),
                projectPage.hasPrevious()
        );
    }

    private String normalizeKeyword(String keyword) {
        log.info("keyword", keyword);
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserProjectEntity createProjectMembership(ProjectEntity project, UserEntity user, int permission, UserEntity grantedBy) {
        UserProjectEntity membership = new UserProjectEntity();
        membership.setProject(project);
        membership.setUser(user);
        membership.setPermission(permission);
        membership.setGrantedByUser(grantedBy);
        return userProjectRepository.save(membership);
    }

    private void validateActorCanManageMembership(ProjectEntity project, String actorId, String actorRole, String actorTenantId) {
        if ("TENANT_ADMIN".equals(actorRole)) {
            TenantAdminEntity tenantAdmin = getTenantAdmin(actorId);
            if (!tenantAdmin.getTenant().getId().equals(project.getTenant().getId())) {
                throw new ForbiddenException("TenantAdmin không cùng tenant với project");
            }
            return;
        }

        if ("USER".equals(actorRole)) {
            if (!project.getOwner().getId().equals(actorId)) {
                throw new ForbiddenException("Chỉ owner của project mới có quyền thêm thành viên");
            }
            if (actorTenantId == null || !actorTenantId.equals(project.getTenant().getId())) {
                throw new ForbiddenException("User không cùng tenant với project");
            }
            return;
        }

        throw new ForbiddenException("Role không được phép thêm user vào project");
    }

    private void validateUserSameTenant(ProjectEntity project, UserEntity userToAdd) {
        if (!project.getTenant().getId().equals(userToAdd.getTenant().getId())) {
            throw new ForbiddenException("User thêm vào phải cùng tenant với project");
        }
    }

    private int normalizePermission(Integer permission) {
        if (permission == null) {
            return PERMISSION_READ;
        }

        if (permission < 1 || permission > PERMISSION_FULL) {
            throw new ConflictException("Permission không hợp lệ, chỉ chấp nhận trong khoảng 1 đến " + PERMISSION_FULL);
        }

        return permission;
    }

    private UserEntity resolveGrantedByUser(String actorId, String actorRole) {
        if (!"USER".equals(actorRole)) {
            return null;
        }
        return getOwner(actorId);
    }

    private ProjectMemberResponse mapToProjectMemberResponse(UserProjectEntity membership) {
        UserEntity member = membership.getUser();
        UserEntity grantedBy = membership.getGrantedByUser();
        return new ProjectMemberResponse(
                membership.getId(),
                membership.getProject().getId(),
                member.getId(),
                member.getUserName(),
                membership.getPermission(),
                grantedBy == null ? null : grantedBy.getId()
        );
    }
}
