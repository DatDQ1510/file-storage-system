package com.java.file_storage_system.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "folders")
public class FolderEntity extends BaseEntity{

    @Column(name = "ownerId", nullable = false)
    private String ownerId;

    @Column(name = "tenantId", nullable = false)
    private String tenantId;

    @Column(name = "parentId")
    private String parentId;

    @Column(name = "nameFolder", nullable = false)
    private String nameFolder;

    @Column(name = "path", nullable = false, columnDefinition = "varchar(255) default '/'")
    private String path;

    @Column(name = "projectId", nullable = false)
    private String projectId;
}
