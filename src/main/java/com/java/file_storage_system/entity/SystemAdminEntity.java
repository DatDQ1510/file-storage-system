package com.java.file_storage_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "systemAdmins")
public class SystemAdminEntity extends BaseEntity {

    @Column(name = "userName", unique = true, nullable = false)
    private String userName;

    @JsonIgnore
    @Column(name = "hashPassword", nullable = false)
    private String hashedPassword;

    @Column(name= "email", unique = true, nullable = false)
    private String email;

}
