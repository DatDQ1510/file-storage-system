package com.java.file_storage_system.custom;

import com.java.file_storage_system.constant.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation để kiểm tra quyền dựa trên role
 * 
 * Cách sử dụng:
 * @RequireRole(UserRole.SYSTEM_ADMIN)
 * @RequireRole({UserRole.SYSTEM_ADMIN, UserRole.TENANT_ADMIN})
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    UserRole[] value() default {};
}
