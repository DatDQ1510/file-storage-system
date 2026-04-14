package com.java.file_storage_system.dto.user.createUser;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserCreatedResponse {
    private String id;
    private String userName;
    private String email;
    private String tenantId;
    private String phoneNumber;
    private String department;
    private LocalDateTime createdAt;
    private BigInteger storage;
    private Boolean MFAEnabled;
}
