package com.java.file_storage_system.dto.tenant;

import com.java.file_storage_system.constant.TenantStatus;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record TenantResponse(
        String id,
        String nameTenant,
        String domainTenant,
        BigInteger exTraStorageSize,
        BigInteger usedStorageSize,
        TenantStatus statusTenant,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}