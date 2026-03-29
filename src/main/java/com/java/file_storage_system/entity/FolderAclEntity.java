package com.java.file_storage_system.entity;

import com.java.file_storage_system.constant.FolderPermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "folderAcl",
        indexes = {
                @Index(name = "idx_folder_acl_folder_id", columnList = "folderId"),
                @Index(name = "idx_folder_acl_user_id", columnList = "userId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_folder_acl_folder_user", columnNames = {"folderId", "userId"})
        }
)
public class FolderAclEntity extends BaseEntity {

    @Column(name = "folderId", nullable = false)
    private UUID folderId;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private FolderPermission permission;
}