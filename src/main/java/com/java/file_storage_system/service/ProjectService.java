package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.project.ProjectRequest;
import com.java.file_storage_system.dto.project.ProjectPageResponse;
import com.java.file_storage_system.dto.project.ProjectResponse;
import com.java.file_storage_system.dto.project.member.AddProjectMemberRequest;
import com.java.file_storage_system.dto.project.member.ProjectMemberResponse;
import com.java.file_storage_system.entity.ProjectEntity;

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
    ProjectResponse getProjectById(String projectId);

    ProjectPageResponse getAllProjectsByTenantAdmin(String tenantAdminId, int page, int size);

    ProjectPageResponse searchProjectsByTenantAdmin(String tenantAdminId, String keyword, int page, int size);

    ProjectMemberResponse addUserToProject(
            String projectId,
            AddProjectMemberRequest request,
            String actorId,
            String actorRole,
            String actorTenantId
    );
}
