package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tenantAdmins")
public class TenantAdminEntity extends BaseEntity {

    @Column(name = "tenantId", nullable = false)
    private String tenantId;

    @Column(name = "userName", unique = true, nullable = false)
    private String userName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "hashPassword", nullable = false)
    private String hashedPassword;

}
