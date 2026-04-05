package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.file.CreateFileRequest;
import com.java.file_storage_system.dto.file.FileResponse;
import com.java.file_storage_system.dto.file.UpdateFileRequest;
import com.java.file_storage_system.entity.FileEntity;

import java.util.List;

public interface FileService extends BaseService<FileEntity> {

	List<FileResponse> getAllFiles();

	FileResponse getFileById(String fileId);

	FileResponse createFile(CreateFileRequest request);

	FileResponse updateFile(String fileId, UpdateFileRequest request);

	void deleteFile(String fileId);
}
