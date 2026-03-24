package com.java.file_storage_system.config;

import com.java.file_storage_system.security.SystemAdminApiInterceptor;
import com.java.file_storage_system.security.TenantAdminApiInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantAdminApiInterceptor tenantAdminApiInterceptor;
    private final SystemAdminApiInterceptor systemAdminApiInterceptor;

    public WebMvcConfig(
            TenantAdminApiInterceptor tenantAdminApiInterceptor,
            SystemAdminApiInterceptor systemAdminApiInterceptor
    ) {
        this.tenantAdminApiInterceptor = tenantAdminApiInterceptor;
        this.systemAdminApiInterceptor = systemAdminApiInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantAdminApiInterceptor)
                .addPathPatterns("/api/v1/users/tenant-admin/register");

        registry.addInterceptor(systemAdminApiInterceptor)
            .addPathPatterns("/api/v1/system-admins/**")
            .excludePathPatterns("/api/v1/system-admins/bootstrap");

        registry.addInterceptor(systemAdminApiInterceptor)
            .addPathPatterns("/api/v1/tenant-admins/**");
    }
}
