package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.FileEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends BaseRepository<FileEntity> {
	Optional<FileEntity> findFirstByTenant_IdAndFolder_IdAndNameFile(String tenantId, String folderId, String nameFile);
	List<FileEntity> findAllByDeletedAtIsNull();
	List<FileEntity> findAllByFolder_IdAndDeletedAtIsNull(String folderId);
	Optional<FileEntity> findByIdAndDeletedAtIsNull(String fileId);
	List<FileEntity> findAllByTenant_IdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String tenantId);
}
