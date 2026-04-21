package com.java.file_storage_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank String username,
    @NotBlank @Email String email
) {
}
