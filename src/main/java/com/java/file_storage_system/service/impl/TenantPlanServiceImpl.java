package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.entity.TenantPlan;
import com.java.file_storage_system.repository.TenantPlanRepository;
import com.java.file_storage_system.service.TenantPlanService;
import org.springframework.stereotype.Service;

@Service
public class TenantPlanServiceImpl extends BaseServiceImpl<TenantPlan, TenantPlanRepository> implements TenantPlanService {

    public TenantPlanServiceImpl(TenantPlanRepository repository) {
        super(repository);
    }
}
