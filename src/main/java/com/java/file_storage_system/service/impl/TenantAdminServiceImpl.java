package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.TenantAdminEntity;
import com.java.file_storage_system.repository.TenantAdminRepository;
import com.java.file_storage_system.service.TenantAdminService;
import org.springframework.stereotype.Service;

@Service
public class TenantAdminServiceImpl extends BaseServiceImpl<TenantAdminEntity, TenantAdminRepository> implements TenantAdminService {

    public TenantAdminServiceImpl(TenantAdminRepository repository) {
        super(repository);
    }
}
