package com.java.file_storage_system.dto.tenantAdmin.check;

public record CheckTenantAdminResponse(
        boolean emailExists,
        boolean sdtExists,
        boolean available
) {
}
