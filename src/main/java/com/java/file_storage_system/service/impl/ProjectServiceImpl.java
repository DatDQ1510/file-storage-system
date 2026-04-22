package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.MessageConstants;
import com.java.file_storage_system.constant.ProjectStatus;
import com.java.file_storage_system.dto.project.ProjectPageResponse;
import com.java.file_storage_system.dto.project.ProjectRequest;
import com.java.file_storage_system.dto.project.ProjectResponse;
import com.java.file_storage_system.dto.project.UpdateProjectRequest;
import com.java.file_storage_system.dto.project.member.AssignProjectMemberRequest;
import com.java.file_storage_system.dto.project.member.ProjectMemberResponse;
import com.java.file_storage_system.dto.project.member.UpdateProjectMemberPermissionRequest;
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

import java.util.ArrayList;
import java.util.Comparator;
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

        // 3. Kiểm tra Owner có phải thành viên của tenant không
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

        validateActorCanManageMembership(project, actorId, actorRole, actorTenantId); // check xem user có quyền của mời thành viên không

        UserEntity userToAdd = getUser(request.memberUserId()); // check member
        validateUserSameTenant(project, userToAdd); // check xem có cùng tenant không

        if (userProjectRepository.existsByUserIdAndProjectId(userToAdd.getId(), project.getId())) { // check xem user có tồn tại trong project chưa
            throw new ConflictException(MessageConstants.PROJECT_USER_ALREADY_MEMBER);
        }

        int permission = normalizePermission(request.permission()); // lấy quyền được truyền xuống
        UserEntity grantedBy = resolveGrantedByUser(actorId, actorRole); // check người chủ mời thành viên mới

        UserProjectEntity membership = createProjectMembership(project, userToAdd, permission, grantedBy);
        return mapToProjectMemberResponse(membership);
    }

    @Override
    public List<ProjectMemberResponse> getProjectMembers(
            String projectId,
            String actorId,
            String actorRole,
            String actorTenantId
    ) {
        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateProjectAccess(project, actorId, actorRole, actorTenantId);

        List<ProjectMemberResponse> members = userProjectRepository.findAllByProjectId(project.getId())
                .stream()
                .map(this::mapToProjectMemberResponse)
                .toList();

        boolean ownerIncluded = members.stream()
                .anyMatch(member -> project.getOwner().getId().equals(member.userId()));

        if (!ownerIncluded) {
            members = new ArrayList<>(members);
            members.add(new ProjectMemberResponse(
                    null,
                    project.getId(),
                    project.getOwner().getId(),
                    project.getOwner().getUserName(),
                    PERMISSION_FULL,
                    null
            ));
        }

        return members.stream()
                .sorted(
                        Comparator.comparing((ProjectMemberResponse member) ->
                                !project.getOwner().getId().equals(member.userId()))
                                .thenComparing(member -> member.userName() == null ? "" : member.userName().toLowerCase())
                )
                .toList();
    }

    @Override
    public ProjectMemberResponse updateProjectMemberPermission(
            String projectId,
            String memberUserId,
            UpdateProjectMemberPermissionRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
    ) {
        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateActorCanManageMembership(project, actorId, actorRole, actorTenantId);
        validateMemberIsNotProjectOwner(project, memberUserId);

        UserProjectEntity member = userProjectRepository.findByUserIdAndProjectId(memberUserId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found"));

        int permission = normalizePermission(request.permission());
        member.setPermission(permission);
        member.setGrantedByUser(resolveGrantedByUser(actorId, actorRole));

        UserProjectEntity updatedMember = userProjectRepository.save(member);
        return mapToProjectMemberResponse(updatedMember);
    }

    @Override
    public void removeProjectMember(
            String projectId,
            String memberUserId,
            String actorId,
            String actorRole,
            String actorTenantId
    ) {
        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateActorCanManageMembership(project, actorId, actorRole, actorTenantId);
        validateMemberIsNotProjectOwner(project, memberUserId);

        UserProjectEntity member = userProjectRepository.findByUserIdAndProjectId(memberUserId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found"));

        userProjectRepository.delete(member);
    }

    @Override
    public ProjectResponse getProjectById(String projectId, String currentUserId, String currentUserRole, String currentTenantId) {
        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateProjectAccess(project, currentUserId, currentUserRole, currentTenantId);
        return mapToResponse(project, currentUserId, currentUserRole, currentTenantId);
    }

    @Override
        public ProjectResponse updateProject(
            String projectId,
            UpdateProjectRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
        ) {
        ProjectEntity project = repository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateActorCanAdminProject(project, actorId, actorRole, actorTenantId);

        String normalizedName = request.nameProject().trim();
        boolean duplicatedName = repository.existsByNameProjectAndTenantId(normalizedName, project.getTenant().getId())
            && !normalizedName.equals(project.getNameProject());

        if (duplicatedName) {
            throw new ConflictException(MessageConstants.PROJECT_ALREADY_EXISTS);
        }

        project.setNameProject(normalizedName);
        ProjectEntity updatedProject = repository.save(project);
        return mapToResponse(updatedProject, actorId, actorRole, actorTenantId);
        }

        @Override
        public void deleteProject(
            String projectId,
            String actorId,
            String actorRole,
            String actorTenantId
        ) {
        ProjectEntity project = repository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));

        validateActorCanAdminProject(project, actorId, actorRole, actorTenantId);
        repository.delete(project);
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


    private UserEntity getUser(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("getOwner called with blank userId");
            throw new ResourceNotFoundException("User không tồn tại");
        }

        String normalizedOwnerId = userId.trim();
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
                throw new ForbiddenException("User không có quyền truy cập project này");
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
            return false;
        }

        if (!"USER".equals(currentUserRole) || currentUserId == null) {
            return false;
        }

        if (currentTenantId != null && !currentTenantId.equals(project.getTenant().getId())) {
            return false;
        }

        if (currentUserId.equals(project.getOwner().getId())) {
            return true;
        }

        return userProjectRepository.findByUserIdAndProjectId(currentUserId, project.getId())
                .map(userProject -> hasPermission(userProject.getPermission(), PERMISSION_MANAGE_MEMBER))
                .orElse(false);
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
            throw new ForbiddenException("Tenant admin không có quyền chỉnh sửa thành viên project");
        }

        if ("USER".equals(actorRole)) {
            if (actorTenantId == null || !actorTenantId.equals(project.getTenant().getId())) {
                throw new ForbiddenException("User không cùng tenant với project");
            }

            if (project.getOwner().getId().equals(actorId)) {
                return;
            }

            boolean hasManagePermission = userProjectRepository.findByUserIdAndProjectId(actorId, project.getId())
                    .map(userProject -> hasPermission(userProject.getPermission(), PERMISSION_MANAGE_MEMBER))
                    .orElse(false);

            if (!hasManagePermission) {
                throw new ForbiddenException("User không có quyền quản lý thành viên trong project");
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

    private void validateActorCanAdminProject(ProjectEntity project, String actorId, String actorRole, String actorTenantId) {
        if (!"USER".equals(actorRole)) {
            throw new ForbiddenException("Role không được phép quản trị project");
        }

        if (actorTenantId == null || !actorTenantId.equals(project.getTenant().getId())) {
            throw new ForbiddenException("User không cùng tenant với project");
        }

        if (project.getOwner().getId().equals(actorId)) {
            return;
        }

        boolean hasAdminPermission = userProjectRepository.findByUserIdAndProjectId(actorId, project.getId())
                .map(userProject -> hasPermission(userProject.getPermission(), PERMISSION_MANAGE_MEMBER))
                .orElse(false);

        if (!hasAdminPermission) {
            throw new ForbiddenException("User không có quyền admin để quản trị project");
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
        return getUser(actorId);
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

    private void validateMemberIsNotProjectOwner(ProjectEntity project, String memberUserId) {
        if (project.getOwner().getId().equals(memberUserId)) {
            throw new ForbiddenException("Project owner cannot be updated or removed");
        }
    }

    private boolean hasPermission(Integer permission, int expectedPermission) {
        if (permission == null) {
            return false;
        }
        return (permission & expectedPermission) == expectedPermission;
    }
}
