package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fileChunkMap")
public class FileChunkMapEntity extends BaseEntity{

    @Column(name = "versionId", nullable = false, comment = "ID of the file version this chunk belongs to", columnDefinition = "INT DEFAULT 1")
    private Integer versionId;

    @Column(name = "chunkId", nullable = false, comment = "ID of the chunk that belongs to the file version")
    private Integer chunkId;

    @Column(name = "orderIndex", nullable = false, comment = "Order of the chunk in the file for reconstruction")
    private Integer orderIndex; // To maintain the order of chunks for reconstructing the file

}
