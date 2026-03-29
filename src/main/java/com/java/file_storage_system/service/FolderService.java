package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.folder.CreateFolderRequest;
import com.java.file_storage_system.dto.folder.FolderResponse;
import com.java.file_storage_system.dto.folder.UpdateFolderRequest;
import com.java.file_storage_system.entity.FolderEntity;

import java.util.List;

public interface FolderService extends BaseService<FolderEntity> {

	List<FolderResponse> getAllFolders();

	FolderResponse getFolderById(String folderId);

	FolderResponse createFolder(CreateFolderRequest request);

	FolderResponse updateFolder(String folderId, UpdateFolderRequest request);

	void deleteFolder(String folderId);
}
