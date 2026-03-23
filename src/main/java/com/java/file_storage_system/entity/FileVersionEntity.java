package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fileVersions")
public class FileVersionEntity extends BaseEntity {

    @Column(name = "fileId", nullable = false)
    private String fileId;

    @Column(name = "versionNumber", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer fileVersion;

    @Column(name = "fileHash", nullable = false, comment = "Hash of the file content for integrity verification")
    private String fileHash;

    @Column(name = "sizeFile", nullable = false, comment = "Size of the file in MB")
    private Double sizeFile; // in MB
}
