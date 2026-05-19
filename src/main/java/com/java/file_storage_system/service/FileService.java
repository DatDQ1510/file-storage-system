package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.file.CreateFileRequest;
import com.java.file_storage_system.dto.file.FileResponse;
import com.java.file_storage_system.dto.file.RecycleBinFileResponse;
import com.java.file_storage_system.dto.file.UpdateFileRequest;
import com.java.file_storage_system.entity.FileEntity;

import java.util.List;

public interface FileService extends BaseService<FileEntity> {

	List<FileResponse> getAllFiles();
	List<FileResponse> getFilesByFolder(String folderId);

	FileResponse getFileById(String fileId);

	FileResponse createFile(CreateFileRequest request);

	FileResponse updateFile(String fileId, UpdateFileRequest request);

	FileResponse renameFile(String fileId, String newName);

	void deleteFile(String fileId);

	List<RecycleBinFileResponse> getRecycleBinFiles();

	void restoreFile(String fileId);

	void permanentlyDeleteFile(String fileId);

	void emptyRecycleBin();
}
