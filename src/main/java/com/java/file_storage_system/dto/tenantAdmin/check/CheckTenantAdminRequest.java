package com.java.file_storage_system.dto.tenantAdmin.check;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CheckTenantAdminRequest(
        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        String email,

        @NotBlank(message = "sdt is required")
        @JsonProperty("sdt")
        String phoneNumber
) {
}
