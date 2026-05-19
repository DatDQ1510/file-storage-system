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
        name = "file_stars",
        indexes = {
                @Index(name = "idx_file_star_file_id", columnList = "fileId"),
                @Index(name = "idx_file_star_user_id", columnList = "userId"),
                @Index(name = "idx_file_star_tenant_id", columnList = "tenantId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_file_star_file_user", columnNames = {"fileId", "userId"})
        }
)
public class FileStarEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fileId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FileEntity file;

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