package com.java.file_storage_system.dto.tenantAdmin.create;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TenantAdminCreatedResponse {
    private String id;
    private String tenantId;
    private String userName;
    private String email;
}