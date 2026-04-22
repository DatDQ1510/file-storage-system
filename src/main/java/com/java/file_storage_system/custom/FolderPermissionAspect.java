package com.java.file_storage_system.custom;

import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.entity.FolderEntity;
import com.java.file_storage_system.entity.UserProjectEntity;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.FolderRepository;
import com.java.file_storage_system.repository.UserProjectRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * AOP Aspect cho @RequireFolderPermission.
 *
 * Logic phân quyền:
 *  - TENANT_ADMIN  → chỉ được READ, không được WRITE/DELETE
 *  - Project owner → bypass hoàn toàn
 *  - USER khác     → kiểm tra folder ACL bitmask trước,
 *                    nếu không có folder ACL thì fallback sang project membership bitmask
 *
 * Quyền cần thiết:
 *  - READ   (bit 1): permission có bit 1 → giá trị 1,3,5,7
 *  - WRITE  (bit 2): permission có bit 2 → giá trị 2,3,6,7
 *  - DELETE (bit 4): permission có bit 4 → giá trị 4,5,6,7
 */
@Aspect
@Component
@RequiredArgsConstructor
public class FolderPermissionAspect {

     private static final int PERMISSION_READ = 1;
    private static final int PERMISSION_WRITE  = 2;
    private static final int PERMISSION_DELETE = 4;

    private final UserContext userContext;
    private final FolderRepository folderRepository;
    private final UserProjectRepository userProjectRepository;

    @Before("@annotation(com.java.file_storage_system.custom.RequireFolderPermission)")
    public void checkFolderPermission(JoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        RequireFolderPermission annotation = method.getAnnotation(RequireFolderPermission.class);

        if (annotation == null) return;

        int requiredBit = switch (annotation.value()) {
            case READ -> PERMISSION_READ;
            case WRITE -> PERMISSION_WRITE;
            case DELETE -> PERMISSION_DELETE;
        };

        // TENANT_ADMIN chỉ được phép đọc folder
        if ("TENANT_ADMIN".equals(userContext.getRole())) {
            if (requiredBit == PERMISSION_READ) {
                String folderId = extractFolderIdArg(sig, joinPoint.getArgs());
                if (folderId == null) {
                    throw new ForbiddenException("Không xác định được folderId để kiểm tra quyền");
                }

                FolderEntity folder = folderRepository.findById(folderId)
                        .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));

                String actorTenantId = userContext.getTenantId();
                if (actorTenantId == null || !actorTenantId.equals(folder.getProject().getTenant().getId())) {
                    throw new ForbiddenException("Tenant admin không cùng tenant với folder");
                }

                return;
            }

            throw new ForbiddenException("Tenant admin không có quyền thao tác WRITE/DELETE trên folder");
        }

        // Chỉ áp dụng với USER
        if (!"USER".equals(userContext.getRole())) {
            throw new ForbiddenException("Role không có quyền thực hiện thao tác này trên folder");
        }

        // Tìm folderId từ tham số method (tên param phải là "folderId")
        String folderId = extractFolderIdArg(sig, joinPoint.getArgs());
        if (folderId == null) {
            throw new ForbiddenException("Không xác định được folderId để kiểm tra quyền");
        }

        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Folder", "id", folderId));

        String actorId    = userContext.getId();
        String actorTenantId = userContext.getTenantId();
        String projectTenantId = folder.getProject().getTenant().getId();

        if (actorTenantId == null || !actorTenantId.equals(projectTenantId)) {
            throw new ForbiddenException("Actor không cùng tenant với folder");
        }

        // Project owner có toàn quyền
        if (folder.getProject().getOwner().getId().equals(actorId)) return;

        // Lấy effective permission: folder ACL → fallback project membership
        Integer effectivePermission = resolveEffectivePermission(folderId, actorId, folder);

        if (effectivePermission == null || (effectivePermission & requiredBit) == 0) {
            String action = annotation.value() == RequireFolderPermission.FolderAction.WRITE ? "WRITE" : "DELETE";
            throw new ForbiddenException(
                    "User không có quyền " + action + " trên folder này. " +
                    "Cần bit " + requiredBit + " trong permission bitmask."
            );
        }
    }

    /**
     * Lấy effective permission:
     * 1. Kiểm tra FolderAcl của actor cho folder này.
     * 2. Nếu không có ACL → fallback sang project membership permission.
     */
    private Integer resolveEffectivePermission(String folderId, String actorId, FolderEntity folder) {
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
     * Lấy giá trị của tham số có tên "folderId" từ method signature.
     */
    private String extractFolderIdArg(MethodSignature sig, Object[] args) {
        Parameter[] params = sig.getMethod().getParameters();
        for (int i = 0; i < params.length; i++) {
            if ("folderId".equals(params[i].getName()) && args[i] instanceof String s) {
                return s;
            }
        }
        return null;
    }
}
