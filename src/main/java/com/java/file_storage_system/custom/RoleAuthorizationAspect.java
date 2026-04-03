package com.java.file_storage_system.custom;

import com.java.file_storage_system.constant.UserRole;
import com.java.file_storage_system.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class RoleAuthorizationAspect {

    /**
     * Kiểm tra role trước khi thực thi method
     */
    @Before("@annotation(com.java.file_storage_system.custom.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        
        if (requireRole == null || requireRole.value().length == 0) {
            return; // Không có role requirement
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("User is not authenticated");
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        UserRole userRole = UserRole.fromString(principal.getRole());
        
        boolean hasRole = Arrays.asList(requireRole.value()).contains(userRole);
        
        if (!hasRole) {
            throw new ForbiddenException(
                String.format("Access denied. Required roles: %s, User role: %s", 
                    Arrays.toString(requireRole.value()), 
                    userRole)
            );
        }
    }
}
