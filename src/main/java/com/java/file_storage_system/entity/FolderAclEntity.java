package com.java.file_storage_system.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folderId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FolderEntity folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    /**
     * Bitmask permission: 1=READ, 2=WRITE, 4=DELETE
     * Synchronized with UserProjectEntity.permission (no MANAGE_MEMBER bit for folders).
     * Valid range: 1–7.
     */
    @Column(name = "permission", nullable = false)
    private Integer permission;
}

