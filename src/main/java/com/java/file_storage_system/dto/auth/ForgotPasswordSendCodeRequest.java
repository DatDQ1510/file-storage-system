package com.java.file_storage_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordSendCodeRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        String email
) {
}
