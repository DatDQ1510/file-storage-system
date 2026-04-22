package com.java.file_storage_system.custom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kiểm tra quyền folder dựa trên bitmask permission của actor trong project.
 *
 * Bitmask: 1=READ, 2=WRITE, 4=DELETE
 *
 * Cách dùng:
 * @RequireFolderPermission(FolderAction.READ)    // đọc:    bit 1 → 1,3,5,7
 * @RequireFolderPermission(FolderAction.WRITE)   // sửa tên: bit 2 → 2,3,6,7
 * @RequireFolderPermission(FolderAction.DELETE)  // xóa:     bit 4 → 4,5,6,7
 *
 * Actor phải có bit tương ứng được set trong permission bitmask.
 * TENANT_ADMIN chỉ được phép với READ, không bypass WRITE/DELETE.
 * Project owner cũng được bypass.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireFolderPermission {
    FolderAction value();

    enum FolderAction {
        /** Bit 1 – đọc folder */
        READ,
        /** Bit 2 – sửa tên folder */
        WRITE,
        /** Bit 4 – xóa folder */
        DELETE
    }
}
