package com.java.file_storage_system.repository;

import com.java.file_storage_system.constant.PlanStatus;
import com.java.file_storage_system.entity.SubscriptionPlanEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends BaseRepository<SubscriptionPlanEntity> {
    List<SubscriptionPlanEntity> findByPlanStatus(PlanStatus planStatus);
}
