package com.java.file_storage_system.dto.systemAdmin.initial;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInitialTenantSetupRequest(
        @NotBlank(message = "nameTenant is required")
        String nameTenant,

        @NotBlank(message = "subdomain is required")
        String subdomain,

        @NotBlank(message = "username is required")
        @Size(max = 100, message = "username must be at most 100 characters")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,

        @JsonAlias({"sdt"})
        @NotBlank(message = "phoneNumber is required")
        String phoneNumber,

        @NotBlank(message = "planId is required")
        String planId
) {
}
