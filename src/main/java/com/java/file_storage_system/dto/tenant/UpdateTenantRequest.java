package com.java.file_storage_system.dto.tenant;

import com.java.file_storage_system.constant.TenantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigInteger;

@Data
public class UpdateTenantRequest {

    @NotBlank(message = "nameTenant is required")
    private String nameTenant;

    @NotBlank(message = "domainTenant is required")
    private String domainTenant;

    @NotNull(message = "exTraStorageSize is required")
    @PositiveOrZero(message = "exTraStorageSize must be greater than or equal to 0")
    private BigInteger exTraStorageSize;

    @NotNull(message = "usedStorageSize is required")
    @PositiveOrZero(message = "usedStorageSize must be greater than or equal to 0")
    private BigInteger usedStorageSize;

    @NotNull(message = "statusTenant is required")
    private TenantStatus statusTenant;
}