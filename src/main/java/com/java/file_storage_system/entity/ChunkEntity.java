package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chunks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"chunk_hash"})
})
public class ChunkEntity extends BaseEntity {

    @Column(name = "chunk_id", nullable = false, unique = true)
    private String chunkId;

    @Column(name = "chunk_hash", nullable = false, length = 64)
    private String chunkHash; // SHA-256

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "object_name", nullable = false)
    private String objectName; // path trong MinIO

    @Column(name = "miniourl", nullable = false)
    private String minIOUrl;

    @Column(name = "bucket", nullable = false)
    private String bucket;

    @Column(name = "etag")
    private String etag; // MinIO trả về

    @Column(name = "status", nullable = false)
    private String status; // UPLOADING, DONE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant;
}
