package com.java.file_storage_system.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "files")
public class FileEntity extends BaseEntity {

    @Column(name = "nameFile", nullable = false)
    private String nameFile;

    @Column(name = "status", nullable = false,
            comment = "0 = DRAFT, 1 = PENDING_REVIEW, 2 = APPROVED, 3 = REJECTED, 4 = DELETED"
    )
    private Integer statusFile;

    @Column(name = "sizeFile", nullable = false, comment = "Size of the file in MB")
    private Double sizeFile; // in MB

    @Column(name = "typeFile", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode ExtraInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenantId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folderId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FolderEntity folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lockedByUserId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity lockedByUser;

    @OneToMany(mappedBy = "file", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<FileVersionEntity> versions = new ArrayList<>();
}
