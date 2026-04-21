package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.PlanStatus;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import com.java.file_storage_system.repository.SubscriptionPlanRepository;
import com.java.file_storage_system.service.SubscriptionPlanService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionPlanServiceImpl extends BaseServiceImpl<SubscriptionPlanEntity, SubscriptionPlanRepository> implements SubscriptionPlanService {

    public SubscriptionPlanServiceImpl(SubscriptionPlanRepository repository) {
        super(repository);
    }

    @Override
    public List<SubscriptionPlanEntity> findByPlanStatus(PlanStatus status) {
        return repository.findByPlanStatus(status);
    }
}
