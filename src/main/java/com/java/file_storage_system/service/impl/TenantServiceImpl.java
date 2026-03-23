package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.service.TenantService;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl extends BaseServiceImpl<TenantEntity, TenantRepository> implements TenantService {

    public TenantServiceImpl(TenantRepository repository) {
        super(repository);
    }
}
