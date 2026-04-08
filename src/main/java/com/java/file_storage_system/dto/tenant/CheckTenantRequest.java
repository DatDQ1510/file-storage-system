package com.java.file_storage_system.dto.tenant;

import jakarta.validation.constraints.NotBlank;

public record CheckTenantRequest(
    @NotBlank(message = "domainTenant is required")
    String domainTenant
) {
}
