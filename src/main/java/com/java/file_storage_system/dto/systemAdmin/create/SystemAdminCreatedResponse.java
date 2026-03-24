package com.java.file_storage_system.dto.systemAdmin.create;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SystemAdminCreatedResponse {
    private String id;
    private String userName;
}