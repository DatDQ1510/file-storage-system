package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fileVersions")
public class FileVersionEntity extends BaseEntity {

    @Column(name = "versionNumber", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer fileVersion;

    @Column(name = "fileHash", nullable = false, comment = "Hash of the file content for integrity verification")
    private String fileHash;

    @Column(name = "sizeFile", nullable = false, comment = "Size of the file in MB")
    private Double sizeFile; // in MB

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fileId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FileEntity file;
}
