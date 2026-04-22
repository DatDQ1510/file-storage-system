package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.FolderAclEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderAclRepository extends BaseRepository<FolderAclEntity> {

    @Query("select fa from FolderAclEntity fa where fa.folder.id = :folderId")
    List<FolderAclEntity> findAllByFolderId(@Param("folderId") String folderId);

    @Query("select fa from FolderAclEntity fa where fa.folder.id = :folderId and fa.user.id = :userId")
    Optional<FolderAclEntity> findByFolderIdAndUserId(@Param("folderId") String folderId, @Param("userId") String userId);
}
