package com.java.file_storage_system.custom;

import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.entity.FileEntity;
import com.java.file_storage_system.entity.FolderEntity;
import com.java.file_storage_system.entity.UserProjectEntity;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FileRepository;
import com.java.file_storage_system.repository.FileShareRepository;
import com.java.file_storage_system.repository.FolderRepository;
import com.java.file_storage_system.repository.UserProjectRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;

/**
 * AOP Aspect cho @RequireFilePermission.
 *
 * Logic phân quyền (hybrid model):
 *  1. File owner → ✅ ALLOW (nếu file không bị lock bởi người khác)
 *  2. Project owner → ✅ ALLOW (bypass)
 *  3. FileShare (valid & not expired) → check FileShare permission
 *  4. Folder ACL → check bitmask
 *  5. Project membership → check bitmask (fallback)
 *  6. ❌ DENY
 *
 * TENANT_ADMIN: chỉ được READ, không WRITE/DELETE
 * USER: tuân theo hybrid logic trên
 *
 * Quyền cần thiết:
 *  - READ   (bit 1): 1,3,5,7 hoặc FileShare (VIEW/COMMENT)
 *  - WRITE  (bit 2): 2,3,6,7 hoặc FileShare (EDIT)
 *  - DELETE (bit 4): 4,5,6,7
 *
 * File lock: nếu @RequireFilePermission(checkFileLock=true)
 *  - lockedByUser != null && lockedByUser != actor → ❌ DENY
 */
@Aspect
@Component
@RequiredArgsConstructor
public class FilePermissionAspect {

    private static final int PERMISSION_READ = 1;
    private static final int PERMISSION_WRITE = 2;
    private static final int PERMISSION_DELETE = 4;

    private final UserContext userContext;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final FileShareRepository fileShareRepository;
    private final UserProjectRepository userProjectRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @Before("@annotation(com.java.file_storage_system.custom.RequireFilePermission)")
    public void checkFilePermission(JoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        RequireFilePermission annotation = method.getAnnotation(RequireFilePermission.class);

        if (annotation == null) return;

        int requiredBit = switch (annotation.value()) {
            case READ -> PERMISSION_READ;
            case WRITE -> PERMISSION_WRITE;
            case DELETE -> PERMISSION_DELETE;
        };

        boolean checkFileLock = annotation.checkFileLock();
        String methodName = method.getName();
        String fileId = extractFileIdArg(sig, joinPoint.getArgs());
        String folderId = extractFolderIdArg(sig, joinPoint.getArgs());
        String actorId = userContext.getId();
        String actorRole = userContext.getRole();
        String actorTenantId = userContext.getTenantId();

        // Endpoint list file hiện tại chưa có resource id để check theo file/folder.
        // Tạm cho phép qua ở tầng aspect, controller/service sẽ xử lý tiếp.
        if (fileId == null && folderId == null) {
            if ("getAllFiles".equals(methodName) && requiredBit == PERMISSION_READ) {
                return;
            }
            throw new ForbiddenException("Không xác định được fileId hoặc folderId để kiểm tra quyền");
        }

        // Nếu thao tác tạo file, request thường chỉ có folderId.
        if (fileId == null) {
            FolderEntity folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));
            String projectTenantId = folder.getProject().getTenant().getId();

            if (actorTenantId == null || !actorTenantId.equals(projectTenantId)) {
                throw new ForbiddenException("Actor không cùng tenant với folder");
            }

            if ("TENANT_ADMIN".equals(actorRole)) {
                if (requiredBit != PERMISSION_READ) {
                    throw new ForbiddenException("Tenant admin không có quyền WRITE/DELETE trên folder");
                }
                return;
            }

            if (!"USER".equals(actorRole)) {
                throw new ForbiddenException("Role không có quyền thực hiện thao tác này trên folder");
            }

            if (folder.getProject().getOwner().getId().equals(actorId)) {
                return;
            }

            Integer effectivePermission = resolveEffectivePermission(folderId, actorId, folder);
            if (effectivePermission == null || (effectivePermission & requiredBit) == 0) {
                throw new ForbiddenException("User không có quyền thao tác trên folder chứa file");
            }
            return;
        }

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> ResourceNotFoundException.byField("File", "id", fileId));

        FolderEntity folder = file.getFolder();
        String projectTenantId = folder.getProject().getTenant().getId();

        // ✅ Kiểm tra tenant consistency
        if (actorTenantId == null || !actorTenantId.equals(projectTenantId)) {
            throw new ForbiddenException("Actor không cùng tenant với file");
        }

        // ✅ TENANT_ADMIN chỉ được READ
        if ("TENANT_ADMIN".equals(actorRole)) {
            if (requiredBit != PERMISSION_READ) {
                throw new ForbiddenException("Tenant admin không có quyền WRITE/DELETE trên file");
            }
            if (checkFileLock && file.getLockedByUser() != null && !file.getLockedByUser().getId().equals(actorId)) {
                throw new ForbiddenException("File bị lock bởi user khác, bạn không được phép thao tác");
            }
            return;
        }

        // ✅ Check file lock (before permission check)
        if (checkFileLock && file.getLockedByUser() != null && !file.getLockedByUser().getId().equals(actorId)) {
            throw new ForbiddenException("File bị lock bởi user khác, bạn không được phép thao tác");
        }

        // ✅ File owner always has access
        if (file.getOwner().getId().equals(actorId)) {
            return;
        }

        // ✅ Project owner always has access
        if (folder.getProject().getOwner().getId().equals(actorId)) {
            return;
        }

        // ✅ Check FileShare (valid & not expired)
        var fileShareOpt = fileShareRepository.findValidFileShare(
                fileId,
                actorId,
                LocalDateTime.now()
        );

        if (fileShareOpt.isPresent()) {
            var fileShare = fileShareOpt.get();
            int sharePermission = mapFileSharePermissionToBit(fileShare.getPermission());

            if ((sharePermission & requiredBit) != 0) {
                return;
            }
            // FileShare không đủ quyền → continue check folder/project
        }

        // ✅ Lấy effective permission từ folder ACL → fallback project membership
        Integer effectivePermission = resolveEffectivePermission(fileId, actorId, folder);

        if (effectivePermission == null || (effectivePermission & requiredBit) == 0) {
            String action = switch (annotation.value()) {
                case READ -> "READ";
                case WRITE -> "WRITE";
                case DELETE -> "DELETE";
            };
            throw new ForbiddenException(
                    "User không có quyền " + action + " trên file này. " +
                    "Cần bit " + requiredBit + " trong permission bitmask hoặc FileShare hợp lệ."
            );
        }
    }

    /**
     * Map FileSharePermission enum → bitmask bit
     * VIEW (1) + COMMENT (1) → READ
     * EDIT (3) → READ + WRITE
     */
    private int mapFileSharePermissionToBit(com.java.file_storage_system.constant.FileSharePermission permission) {
        return switch (permission) {
            case VIEW -> 1;      // READ only
            case COMMENT -> 1;   // READ only (has comment action)
            case EDIT -> 3;      // READ + WRITE (no DELETE)
        };
    }

    /**
     * Lấy effective permission từ folder/project:
     * 1. Kiểm tra FolderAcl của actor cho folder này.
     * 2. Nếu không có ACL → fallback sang project membership permission.
     */
    private Integer resolveEffectivePermission(String fileId, String actorId, FolderEntity folder) {
        String folderId = folder.getId();

        // Try folder ACL first
        var folderAclOpt = folderRepository.findFolderAclPermission(folderId, actorId);
        if (folderAclOpt.isPresent()) {
            return folderAclOpt.get();
        }

        // Fallback to project membership permission
        return userProjectRepository.findByUserIdAndProjectId(actorId, folder.getProject().getId())
                .map(UserProjectEntity::getPermission)
                .orElse(null);
    }

    /**
     * Lấy giá trị của tham số có tên "fileId" từ method signature.
     */
    private String extractFileIdArg(MethodSignature sig, Object[] args) {
        return extractStringArg(sig, args, "fileId");
    }

    /**
     * Lấy giá trị folderId từ tham số method hoặc từ record/request object.
     */
    private String extractFolderIdArg(MethodSignature sig, Object[] args) {
        return extractStringArg(sig, args, "folderId");
    }

    private String extractStringArg(MethodSignature sig, Object[] args, String targetName) {
        Parameter[] params = sig.getMethod().getParameters();
        for (int i = 0; i < params.length; i++) {
            if (targetName.equals(params[i].getName())) {
                Object arg = args[i];
                if (arg instanceof String s) {
                    return s;
                }
            }
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            try {
                var accessor = arg.getClass().getMethod(targetName);
                Object value = accessor.invoke(arg);
                if (value instanceof String s && !s.isBlank()) {
                    return s;
                }
            } catch (ReflectiveOperationException ignored) {
                // ignore and try field access
            }

            try {
                Field field = arg.getClass().getDeclaredField(targetName);
                field.setAccessible(true);
                Object value = field.get(arg);
                if (value instanceof String s && !s.isBlank()) {
                    return s;
                }
            } catch (ReflectiveOperationException ignored) {
                // ignore and continue searching
            }
        }

        return null;
    }
}
