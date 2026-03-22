package com.java.file_storage_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "systemAdmins")
public class SystemAdminEntity extends BaseEntity {

    @Column(name = "userName", unique = true, nullable = false)
    private String username;

    @JsonIgnore
    @Column(name = "hashPassword", nullable = false)
    private String hashPassword;

}
