package com.java.file_storage_system.config;

import com.java.file_storage_system.security.TenantAdminApiInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantAdminApiInterceptor tenantAdminApiInterceptor;

    public WebMvcConfig(TenantAdminApiInterceptor tenantAdminApiInterceptor) {
        this.tenantAdminApiInterceptor = tenantAdminApiInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantAdminApiInterceptor)
                .addPathPatterns("/api/v1/users/tenant-admin/register");
    }
}
