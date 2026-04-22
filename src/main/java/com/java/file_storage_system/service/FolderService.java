package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.folder.CreateFolderRequest;
import com.java.file_storage_system.dto.folder.CreateFolderWithAclRequest;
import com.java.file_storage_system.dto.folder.CreateFolderWithAclResponse;
import com.java.file_storage_system.dto.folder.FolderAclItemResponse;
import com.java.file_storage_system.dto.folder.FolderPathNodeResponse;
import com.java.file_storage_system.dto.folder.FolderResponse;
import com.java.file_storage_system.dto.folder.ProjectMemberForAclResponse;
import com.java.file_storage_system.dto.folder.RenameFolderRequest;
import com.java.file_storage_system.dto.folder.UpdateFolderRequest;
import com.java.file_storage_system.dto.folder.UpsertFolderAclRequest;
import com.java.file_storage_system.entity.FolderEntity;

import java.util.List;

public interface FolderService extends BaseService<FolderEntity> {

	List<FolderResponse> getAllFolders();

	FolderResponse getFolderById(String folderId);

	FolderResponse createFolder(CreateFolderRequest request);

	FolderResponse updateFolder(String folderId, UpdateFolderRequest request);

	void deleteFolder(String folderId);

	List<FolderResponse> getFoldersByProject(String projectId, String actorId, String actorRole, String actorTenantId);

	CreateFolderWithAclResponse createFolderWithAcl(
			String projectId,
			CreateFolderWithAclRequest request,
			String actorId,
			String actorRole,
			String actorTenantId
	);

	List<FolderPathNodeResponse> getChildFolderPaths(
			String projectId,
			String parentPath,
			String actorId,
			String actorRole,
			String actorTenantId
	);

	List<FolderPathNodeResponse> searchFolderPaths(
			String projectId,
			String keyword,
			String actorId,
			String actorRole,
			String actorTenantId
	);

	List<ProjectMemberForAclResponse> getProjectMembersForAcl(
			String projectId,
			String actorId,
			String actorRole,
			String actorTenantId
	);

	List<FolderAclItemResponse> getFolderAcl(
			String folderId,
			String actorId,
			String actorRole,
			String actorTenantId
	);

	FolderAclItemResponse upsertFolderAcl(
			String folderId,
			String userId,
			UpsertFolderAclRequest request,
			String actorId,
			String actorRole,
			String actorTenantId
	);

	/**
	 * Đổi tên folder – yêu cầu actor có quyền WRITE (bit 2).
	 * AOP @RequireFolderPermission(WRITE) đã kiểm tra trước khi vào method này.
	 */
	FolderResponse renameFolder(String folderId, RenameFolderRequest request,
			String actorId, String actorRole, String actorTenantId);

	/**
	 * Xóa folder – yêu cầu actor có quyền DELETE (bit 4).
	 * AOP @RequireFolderPermission(DELETE) đã kiểm tra trước khi vào method này.
	 */
	void deleteFolderByActor(String folderId, String actorId, String actorRole, String actorTenantId);

	/**
	 * Lấy danh sách folder con trực tiếp (direct children) theo parentId.
	 * parentFolderId = null → lấy các folder ở root (parentFolder IS NULL).
	 */
	List<FolderResponse> getFoldersByParentId(
			String projectId,
			String parentFolderId,
			String actorId,
			String actorRole,
			String actorTenantId
	);
}

