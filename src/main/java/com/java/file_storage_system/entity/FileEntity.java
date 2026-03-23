package com.java.file_storage_system.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "files")
public class FileEntity extends BaseEntity {

    @Column(name = "tenantId", nullable = false)
    private String tenantId;

    @Column(name = "folderId", nullable = false, columnDefinition = "varchar(255) default '/'") // Default to root folder if not specified
    private String folderId;

    @Column(name = "ownerId", nullable = false)
    private String ownerId;

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

    @Column(name = "lockedByUserId", nullable = true, comment = "User ID of the user who has locked the file for editing")
    private String lockedByUserId;
}
