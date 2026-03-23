package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigInteger;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "tenants")
public class TenantEntity extends BaseEntity {

    @Column(name = "nameTenant", nullable = false, unique = true)
    private String nameTenant;

    @Column(name = "domainTenant", nullable = false, unique = true)
    private String domainTenant;

    @Column(name = "exTraStorageSize", nullable = false,
            comment = "Tenant purchases additional storage space beyond the default allocation." +
                    " This field tracks the extra storage size in bytes that the tenant has acquired," +
                    " allowing for flexible storage management and billing based on usage.")
    private BigInteger exTraStorageSize;

    @Column(name = "usedStorageSize", nullable = false)
    private BigInteger usedStorageSize;

    @Column(name = "status", nullable = false,
            comment = "0 = INACTIVE, 1 = ACTIVE, 2 = SUSPENDED, 3 = DELETED"
    )
    private Integer statusTenant;



}
