package com.java.file_storage_system.dto.user.createUser;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCreatedResponse {
    private String id;
    private String userName;
    private String email;
    private String tenantId;
}
