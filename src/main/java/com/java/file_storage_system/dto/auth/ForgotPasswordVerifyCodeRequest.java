package com.java.file_storage_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordVerifyCodeRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        String email,

        @NotBlank(message = "code is required")
        @Pattern(regexp = "^\\d{6}$", message = "code must be exactly 6 digits")
        String code
) {
}
