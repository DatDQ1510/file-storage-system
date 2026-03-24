package com.java.file_storage_system.dto.user.createUser;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTenantUserRequest {

    @NotBlank(message = "userName is required")
    @Size(max = 100, message = "userName must be at most 100 characters")
    private String userName;

    @NotBlank(message = "email is required")
    @Email(message = "email is invalid")
    @Size(max = 255, message = "email must be at most 255 characters")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100, message = "password must be between 6 and 100 characters")
    private String password;
}
