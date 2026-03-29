package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.PaymentTransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends BaseRepository<PaymentTransactionEntity> {

    Optional<PaymentTransactionEntity> findByProviderTransactionId(String providerTransactionId);

    List<PaymentTransactionEntity> findByTenant_IdOrderByCreatedAtDesc(String tenantId);
}
