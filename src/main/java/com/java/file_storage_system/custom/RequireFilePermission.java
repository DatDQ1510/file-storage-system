package com.java.file_storage_system.custom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kiểm tra quyền file dựa trên:
 * 1. Folder ACL của file (cascade)
 * 2. Project membership (fallback)
 * 3. FileShare permission (if valid/not expired)
 *
 * Bitmask: 1=READ, 2=WRITE, 4=DELETE (giống folder)
 *
 * Cách dùng:
 * @RequireFilePermission(FileAction.READ)    // xem file:    bit 1 → 1,3,5,7
 * @RequireFilePermission(FileAction.WRITE)   // upload:      bit 2 → 2,3,6,7
 * @RequireFilePermission(FileAction.DELETE)  // xóa file:     bit 4 → 4,5,6,7
 *
 * Permission check order:
 *  1. File owner → ✅ ALLOW (nếu file không bị lock bởi người khác)
 *  2. Project owner → ✅ ALLOW (bypass)
 *  3. FileShare (valid & not expired) → check FileShare permission
 *  4. Folder ACL → check bitmask
 *  5. Project membership → check bitmask (fallback)
 *  6. ❌ DENY
 *
 * TENANT_ADMIN: chỉ READ, không WRITE/DELETE
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireFilePermission {
    FileAction value();

    /**
     * Có check file lock không?
     * Nếu true: locked by other user → ❌ DENY (kể cả có permission)
     * Default: true
     */
    boolean checkFileLock() default true;

    enum FileAction {
        /** Bit 1 – đọc file, download, view metadata */
        READ,
        /** Bit 2 – upload version, rename, move, update metadata */
        WRITE,
        /** Bit 4 – xóa file */
        DELETE
    }
}
