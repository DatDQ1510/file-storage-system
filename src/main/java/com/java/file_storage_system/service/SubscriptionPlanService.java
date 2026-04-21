package com.java.file_storage_system.service;

import com.java.file_storage_system.constant.PlanStatus;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;

import java.util.List;

public interface SubscriptionPlanService extends BaseService<SubscriptionPlanEntity> {
    List<SubscriptionPlanEntity> findByPlanStatus(PlanStatus status);
}
