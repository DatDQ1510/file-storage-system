package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.FolderEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends BaseRepository<FolderEntity> {

    @Query("select f from FolderEntity f where f.project.id = :projectId order by f.updatedAt desc")
    List<FolderEntity> findAllByProjectId(@Param("projectId") String projectId);

    @Query("select f from FolderEntity f where f.project.id = :projectId and f.path = :path")
    Optional<FolderEntity> findByProjectIdAndPath(@Param("projectId") String projectId, @Param("path") String path);

    @Query("select (count(f) > 0) from FolderEntity f where f.project.id = :projectId and f.path = :path")
    boolean existsByProjectIdAndPath(@Param("projectId") String projectId, @Param("path") String path);

    @Query("""
            select f
            from FolderEntity f
            where f.project.id = :projectId
            and lower(f.path) like lower(concat('%', :keyword, '%'))
            order by f.path asc
            """)
    List<FolderEntity> searchByProjectIdAndPathKeyword(
            @Param("projectId") String projectId,
            @Param("keyword") String keyword
    );

    /**
     * Lấy permission bitmask của actor trong folder ACL.
     * Trả về Optional.empty() nếu không có ACL entry cho actor này.
     */
    @Query("select fa.permission from FolderAclEntity fa where fa.folder.id = :folderId and fa.user.id = :userId")
    Optional<Integer> findFolderAclPermission(@Param("folderId") String folderId, @Param("userId") String userId);
}

