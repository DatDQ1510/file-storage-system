package com.java.file_storage_system.context;

import com.java.file_storage_system.custom.CustomUserDetails;
import com.java.file_storage_system.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Slf4j
@Component
public class UserContext {

    /**
     * Lấy user hiện tại từ SecurityContext
     */
    public CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        throw new UnauthorizedException("User chưa được xác thực");
    }

    /**
     * Lấy user hiện tại (Optional)
     */
    public Optional<CustomUserDetails> getCurrentUserOptional() {
        try {
            return Optional.of(getCurrentUser());
        } catch (UnauthorizedException e) {
            return Optional.empty();
        }
    }

    /**
     * Lấy ID của user hiện tại
     */
    public String getId() {
        return getCurrentUser().getId();
    }

    /**
     * Lấy username của user hiện tại
     */
    public String getUsername() {
        return getCurrentUser().getUsername();
    }

    /**
     * Lấy role của user hiện tại
     */
    public String getRole() {
        return getCurrentUser().getRole();
    }

    /**
     * Lấy tenant ID của user hiện tại
     */
    public String getTenantId() {
        return getCurrentUser().getTenantId();
    }

    /**
     * Lấy JWT token từ request header
     */
    public String getAuthorization() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("Không tìm thấy request attributes");
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Lấy Authorization header
     */
    public String getAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getHeader("Authorization");
    }

    /**
     * Kiểm tra user đã xác thực chưa
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String);
    }

    /**
     * Kiểm tra user có role cụ thể không
     */
    public boolean hasRole(String role) {
        try {
            return getRole().equals(role);
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    /**
     * Kiểm tra user có phải SYSTEM_ADMIN không
     */
    public boolean isSystemAdmin() {
        return hasRole("SYSTEM_ADMIN");
    }

    /**
     * Kiểm tra user có phải TENANT_ADMIN không
     */
    public boolean isTenantAdmin() {
        return hasRole("TENANT_ADMIN");
    }

    /**
     * Kiểm tra user có phải USER không
     */
    public boolean isRegularUser() {
        return hasRole("USER");
    }

    /**
     * Lấy thông tin đầy đủ của user hiện tại
     */
    public CustomUserDetails getUserDetails() {
        return getCurrentUser();
    }

    /**
     * Xóa authentication khỏi context
     */
    public void clearContext() {
        SecurityContextHolder.clearContext();
        log.info("Đã xóa security context");
    }

    /**
     * Lấy thông tin user dưới dạng String
     */
    public String getUserInfo() {
        try {
            CustomUserDetails user = getCurrentUser();
            return String.format("User: %s (ID: %s, Role: %s, TenantId: %s)",
                    user.getUsername(), user.getId(), user.getRole(), user.getTenantId());
        } catch (UnauthorizedException e) {
            return "User không xác thực";
        }
    }
}
