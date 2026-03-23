package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "projects")
public class ProjectEntity extends BaseEntity {

    @Column(name = "tenantId", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false)
    private String nameProject;

    @Column(name = "ownerId", nullable = false)
    private String ownerId;

}
