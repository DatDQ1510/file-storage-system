package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.FileStarEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileStarRepository extends BaseRepository<FileStarEntity> {

    Optional<FileStarEntity> findByFile_IdAndUser_Id(@Param("fileId") String fileId, @Param("userId") String userId);

    boolean existsByFile_IdAndUser_Id(@Param("fileId") String fileId, @Param("userId") String userId);

    void deleteByFile_IdAndUser_Id(@Param("fileId") String fileId, @Param("userId") String userId);

    @Query("select fs from FileStarEntity fs where fs.user.id = :userId and fs.tenant.id = :tenantId order by fs.createdAt desc")
    List<FileStarEntity> findAllByUserAndTenantOrderByCreatedAtDesc(
            @Param("userId") String userId,
            @Param("tenantId") String tenantId
    );
}