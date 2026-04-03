package com.java.file_storage_system.custom;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Data
@NullMarked
public class CustomUserDetails implements UserDetails {
    private final String id;
    private final String username;
    private final String password;
    private final String role;
    private final String tenantId;
    private final String email;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }
}
