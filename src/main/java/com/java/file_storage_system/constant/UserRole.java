package com.java.file_storage_system.constant;

public enum UserRole {
    SYSTEM_ADMIN("SYSTEM_ADMIN", "Quản lý hệ thống"),
    TENANT_ADMIN("TENANT_ADMIN", "Quản lý công ty"),
    USER("USER", "Người dùng bình thường");

    private final String roleCode;
    private final String description;

    UserRole(String roleCode, String description) {
        this.roleCode = roleCode;
        this.description = description;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromString(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.roleCode.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
