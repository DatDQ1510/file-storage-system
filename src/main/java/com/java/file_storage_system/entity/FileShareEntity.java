package com.java.file_storage_system.entity;

import com.java.file_storage_system.constant.FileSharePermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "fileShares",
        indexes = {
                @Index(name = "idx_file_shares_file_id", columnList = "fileId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_file_share_file_user", columnNames = {"fileId", "sharedWithUserId"})
        }
)
public class FileShareEntity extends BaseEntity {

    @Column(name = "fileId", nullable = false)
    private UUID fileId;

    @Column(name = "sharedWithUserId", nullable = false)
    private UUID sharedWithUserId;

    @Column(name = "sharedByUserId", nullable = false)
    private UUID sharedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private FileSharePermission permission;

    @Column(name = "expiresAt")
    private LocalDateTime expiresAt;
}