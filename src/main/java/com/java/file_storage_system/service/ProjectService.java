package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.project.ProjectRequest;
import com.java.file_storage_system.dto.project.ProjectPageResponse;
import com.java.file_storage_system.dto.project.ProjectResponse;
import com.java.file_storage_system.dto.project.UpdateProjectRequest;
import com.java.file_storage_system.dto.project.member.AssignProjectMemberRequest;
import com.java.file_storage_system.dto.project.member.ProjectMemberResponse;
import com.java.file_storage_system.dto.project.member.UpdateProjectMemberPermissionRequest;
import com.java.file_storage_system.entity.ProjectEntity;

import java.util.List;

public interface ProjectService extends BaseService<ProjectEntity> {

    /**
     * Tạo project mới với điều kiện:
     * - Người dùng phải là TenantAdmin
     * - ownerId phải là thành viên của tenant
     * - Tenant của TenantAdmin phải trùng với tenant của owner
     *
     * @param request ProjectRequest chứa nameProject và ownerId
     * @param tenantAdminId ID của TenantAdmin tạo project
     * @return ProjectResponse
     * @throws UnauthorizedException nếu TenantAdmin không hợp lệ
     * @throws ForbiddenException nếu ownerId không phải thành viên của tenant
     * @throws ConflictException nếu project đã tồn tại
     */
    ProjectResponse createProject(ProjectRequest request, String tenantAdminId);

    /**
     * Lấy thông tin project
     *
     * @param projectId ID của project
     * @return ProjectResponse
     */
    ProjectResponse getProjectById(String projectId, String currentUserId, String currentUserRole, String currentTenantId);

    ProjectPageResponse getAllProjectsByUser(String userId, int page, int size);

    ProjectPageResponse searchProjectsByTenantAdmin(String tenantAdminId, String keyword, int page, int size);

        ProjectResponse updateProject(
            String projectId,
            UpdateProjectRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
        );

        void deleteProject(
            String projectId,
            String actorId,
            String actorRole,
            String actorTenantId
        );

        ProjectMemberResponse assignMemberToProject(
            String projectId,
            AssignProjectMemberRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
        );

    List<ProjectMemberResponse> getProjectMembers(
            String projectId,
            String actorId,
            String actorRole,
            String actorTenantId
    );

    ProjectMemberResponse updateProjectMemberPermission(
            String projectId,
            String memberUserId,
            UpdateProjectMemberPermissionRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
    );

    void removeProjectMember(
            String projectId,
            String memberUserId,
            String actorId,
            String actorRole,
            String actorTenantId
    );
}
