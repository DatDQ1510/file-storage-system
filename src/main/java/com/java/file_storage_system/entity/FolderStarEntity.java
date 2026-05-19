package com.java.file_storage_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
        name = "folder_stars",
        indexes = {
                @Index(name = "idx_folder_star_folder_id", columnList = "folderId"),
                @Index(name = "idx_folder_star_user_id", columnList = "userId"),
                @Index(name = "idx_folder_star_tenant_id", columnList = "tenantId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_folder_star_folder_user", columnNames = {"folderId", "userId"})
        }
)
public class FolderStarEntity extends BaseEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenantId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TenantEntity tenant;
}