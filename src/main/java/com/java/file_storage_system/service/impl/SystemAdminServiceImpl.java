package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.SystemAdminEntity;
import com.java.file_storage_system.repository.SystemAdminRepository;
import com.java.file_storage_system.service.SystemAdminService;
import org.springframework.stereotype.Service;

@Service
public class SystemAdminServiceImpl extends BaseServiceImpl<SystemAdminEntity, SystemAdminRepository> implements SystemAdminService {

    public SystemAdminServiceImpl(SystemAdminRepository repository) {
        super(repository);
    }
}
