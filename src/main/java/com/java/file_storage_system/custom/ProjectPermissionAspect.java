package com.java.file_storage_system.custom;

import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.entity.ProjectEntity;
import com.java.file_storage_system.entity.UserProjectEntity;
import com.java.file_storage_system.exception.ForbiddenException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.ProjectRepository;
import com.java.file_storage_system.repository.UserProjectRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
@RequiredArgsConstructor
public class ProjectPermissionAspect {

    private final UserContext userContext;
    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;

    @Before("@annotation(com.java.file_storage_system.custom.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);

        if (requirePermission == null) {
            return;
        }

        if (userContext.isTenantAdmin()) {
            throw new ForbiddenException("Tenant admin không có quyền thao tác chức năng này");
        }

        if (!userContext.isRegularUser()) {
            throw new ForbiddenException("Role không được phép thao tác chức năng này");
        }

        String projectId = extractProjectIdArg(signature, joinPoint.getArgs());
        if (projectId == null || projectId.isBlank()) {
            throw new ForbiddenException("Không xác định được projectId để kiểm tra quyền");
        }

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project không tồn tại"));

        String actorId = userContext.getId();
        String actorTenantId = userContext.getTenantId();

        if (actorTenantId == null || !actorTenantId.equals(project.getTenant().getId())) {
            throw new ForbiddenException("User không cùng tenant với project");
        }

        if (project.getOwner().getId().equals(actorId)) {
            return;
        }

        UserProjectEntity membership = userProjectRepository.findByUserIdAndProjectId(actorId, projectId)
                .orElseThrow(() -> new ForbiddenException("User không thuộc project này"));

        int requiredBit = requirePermission.value().bit();
        Integer permission = membership.getPermission();

        if (permission == null || (permission & requiredBit) != requiredBit) {
            throw new ForbiddenException("User không có quyền phù hợp để thao tác chức năng này");
        }
    }

    private String extractProjectIdArg(MethodSignature signature, Object[] args) {
        Parameter[] params = signature.getMethod().getParameters();
        for (int i = 0; i < params.length; i++) {
            if ("projectId".equals(params[i].getName()) && args[i] instanceof String projectId) {
                return projectId;
            }
        }
        return null;
    }
}
