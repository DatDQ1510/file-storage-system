package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.FolderStarEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderStarRepository extends BaseRepository<FolderStarEntity> {

    Optional<FolderStarEntity> findByFolder_IdAndUser_Id(@Param("folderId") String folderId, @Param("userId") String userId);

    boolean existsByFolder_IdAndUser_Id(@Param("folderId") String folderId, @Param("userId") String userId);

    void deleteByFolder_IdAndUser_Id(@Param("folderId") String folderId, @Param("userId") String userId);

    @Query("select fs from FolderStarEntity fs where fs.user.id = :userId and fs.tenant.id = :tenantId order by fs.createdAt desc")
    List<FolderStarEntity> findAllByUserAndTenantOrderByCreatedAtDesc(
            @Param("userId") String userId,
            @Param("tenantId") String tenantId
    );
}