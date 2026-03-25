package com.java.file_storage_system.custom;

import com.java.file_storage_system.entity.SystemAdminEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.entity.UserEntity;
import com.java.file_storage_system.repository.SystemAdminRepository;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
@RequiredArgsConstructor
@NullMarked
public class CustomUserDetailsService implements UserDetailsService {

    private final SystemAdminRepository systemAdminRepository;
    private final TenantAdminRepository tenantAdminRepository;
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        String normalized = userName.trim().toLowerCase();

        SystemAdminEntity systemAdmin = systemAdminRepository.findByUserNameIgnoreCase(normalized).orElse(null);
        if (systemAdmin != null) {
            return new CustomUserDetails(
                    systemAdmin.getId(),
                    systemAdmin.getUserName(),
                    "SYSTEM_ADMIN",
                    null
            );
        }

        TenantAdminEntity tenantAdmin = tenantAdminRepository.findByEmailIgnoreCase(normalized)
                .or(() -> tenantAdminRepository.findByUserNameIgnoreCase(normalized))
                .orElse(null);
        if (tenantAdmin != null) {
            return new CustomUserDetails(
                    tenantAdmin.getId(),
                    tenantAdmin.getUserName(),
                    "TENANT_ADMIN",
                    tenantAdmin.getTenant() == null ? null : tenantAdmin.getTenant().getId()
            );
        }

        UserEntity user = userRepository.findByEmailIgnoreCase(normalized)
                .or(() -> userRepository.findByUserNameIgnoreCase(normalized))
                .orElse(null);
        if (user != null) {
            return new CustomUserDetails(
                    user.getId(),
                    user.getUserName(),
                    "USER",
                    user.getTenant() == null ? null : user.getTenant().getId()
            );
        }

        throw new UsernameNotFoundException("User not found: " + userName);
    }
}
