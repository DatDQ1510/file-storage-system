package com.java.file_storage_system.dto.user.allUser;

import com.java.file_storage_system.constant.UserStatus;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record AllUserResponse(
        String userName,
        String email,
        String phoneNumber,
        String department,
        LocalDateTime createdAt,
        UserStatus status,
        Boolean MFAEnabled,
        BigInteger storage
) {
}
