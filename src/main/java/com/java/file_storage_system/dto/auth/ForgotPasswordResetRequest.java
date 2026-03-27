package com.java.file_storage_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordResetRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        String email,

        @NotBlank(message = "code is required")
        @Pattern(regexp = "^\\d{6}$", message = "code must be exactly 6 digits")
        String code,

        @NotBlank(message = "newPassword is required")
        @Size(min = 6, max = 100, message = "newPassword must be between 6 and 100 characters")
        String newPassword
) {
}
