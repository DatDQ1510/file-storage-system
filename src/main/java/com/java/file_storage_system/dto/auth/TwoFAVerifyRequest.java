package com.java.file_storage_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFAVerifyRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "code is required")
        @Pattern(regexp = "\\d{6}", message = "code must contain exactly 6 digits")
        String code
) {
}