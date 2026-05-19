package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.FileShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShareEntity, String> {

    /**
     * Tìm FileShare giữa file và user.
     * Không check expiry - caller cần check
     */
    @Query("SELECT fs FROM FileShareEntity fs " +
           "WHERE fs.fileId = :fileId AND fs.sharedWithUserId = :sharedWithUserId")
    Optional<FileShareEntity> findByFileIdAndSharedWithUserId(
           @Param("fileId") String fileId,
           @Param("sharedWithUserId") String sharedWithUserId
    );

    /**
     * Tìm FileShare hợp lệ (chưa expire).
     * Nếu expiresAt = null → không expire
     */
    @Query("SELECT fs FROM FileShareEntity fs " +
           "WHERE fs.fileId = :fileId " +
           "AND fs.sharedWithUserId = :sharedWithUserId " +
           "AND (fs.expiresAt IS NULL OR fs.expiresAt > :now)")
    Optional<FileShareEntity> findValidFileShare(
           @Param("fileId") String fileId,
           @Param("sharedWithUserId") String sharedWithUserId,
            @Param("now") LocalDateTime now
    );

    /**
     * Lấy tất cả FileShare cho một file (không filter expiry)
     */
    @Query("SELECT fs FROM FileShareEntity fs " +
           "WHERE fs.fileId = :fileId " +
           "ORDER BY fs.createdAt DESC")
    List<FileShareEntity> findAllByFileId(@Param("fileId") String fileId);

    /**
     * Lấy tất cả FileShare hợp lệ cho một file (chưa expire)
     */
    @Query("SELECT fs FROM FileShareEntity fs " +
           "WHERE fs.fileId = :fileId " +
           "AND (fs.expiresAt IS NULL OR fs.expiresAt > :now) " +
           "ORDER BY fs.createdAt DESC")
    List<FileShareEntity> findValidFileShares(
           @Param("fileId") String fileId,
            @Param("now") LocalDateTime now
    );

    /**
     * Đếm FileShare hợp lệ cho một file
     */
    @Query("SELECT COUNT(fs) FROM FileShareEntity fs " +
           "WHERE fs.fileId = :fileId " +
           "AND (fs.expiresAt IS NULL OR fs.expiresAt > :now)")
    long countValidFileShares(
           @Param("fileId") String fileId,
            @Param("now") LocalDateTime now
    );

    /**
     * Xoá FileShare giữa file và user
     */
       void deleteByFileIdAndSharedWithUserId(String fileId, String sharedWithUserId);

    /**
     * Xoá tất cả FileShare của file (khi file bị xoá)
     */
       void deleteByFileId(String fileId);

    /**
     * Tìm tất cả FileShare đã expire
     */
    @Query("SELECT fs FROM FileShareEntity fs " +
           "WHERE fs.expiresAt IS NOT NULL AND fs.expiresAt <= :now")
    List<FileShareEntity> findExpiredShares(@Param("now") LocalDateTime now);
}
