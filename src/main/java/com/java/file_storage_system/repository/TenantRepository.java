package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.TenantEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends BaseRepository<TenantEntity> {

	boolean existsByNameTenant(String nameTenant);

	boolean existsByDomainTenant(String domainTenant);

	boolean existsByNameTenantAndIdNot(String nameTenant, String id);

	boolean existsByDomainTenantAndIdNot(String domainTenant, String id);
}
