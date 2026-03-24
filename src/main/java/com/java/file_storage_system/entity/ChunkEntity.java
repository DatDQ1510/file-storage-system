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
@Table(name = "chunks")
public class ChunkEntity extends BaseEntity{

    @Column(name = "chunkId", nullable = false, unique = true)
    private Double sizeChunk;

    @Column(name = "minIOUrl", nullable = false, comment = "URL to access the chunk in MinIO storage")
    private String minIOUrl;

    @Column(name = "chunkHash", nullable = false, comment = "Hash of the chunk content for integrity verification")
    private String chunkHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenantId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TenantEntity tenant;
}
