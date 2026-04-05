package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.fileVersion.CreateFileVersionRequest;
import com.java.file_storage_system.dto.fileVersion.FileVersionResponse;
import com.java.file_storage_system.dto.fileVersion.UpdateFileVersionRequest;
import com.java.file_storage_system.entity.FileVersionEntity;

import java.util.List;

public interface FileVersionService extends BaseService<FileVersionEntity> {

	List<FileVersionResponse> getAllFileVersions();

	FileVersionResponse getFileVersionById(String fileVersionId);

	FileVersionResponse createFileVersion(CreateFileVersionRequest request);

	FileVersionResponse updateFileVersion(String fileVersionId, UpdateFileVersionRequest request);

	void deleteFileVersion(String fileVersionId);
}
