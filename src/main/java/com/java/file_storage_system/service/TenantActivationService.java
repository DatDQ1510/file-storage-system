package com.java.file_storage_system.service;

import com.java.file_storage_system.dto.tenant.activation.TenantActivationCompleteRequest;
import com.java.file_storage_system.dto.tenant.activation.TenantActivationCompleteResponse;
import com.java.file_storage_system.dto.tenant.activation.TenantActivationTokenInfo;
import com.java.file_storage_system.entity.TenantAdminEntity;

public interface TenantActivationService {

    void createActivationInvitation(TenantAdminEntity tenantAdmin);

    TenantActivationTokenInfo validateActivationToken(String token);

    TenantActivationCompleteResponse completeActivation(TenantActivationCompleteRequest request);
}
