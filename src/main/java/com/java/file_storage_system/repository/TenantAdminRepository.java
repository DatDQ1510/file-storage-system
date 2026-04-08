package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.TenantAdminEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantAdminRepository extends BaseRepository<TenantAdminEntity> {

	boolean existsByUserNameIgnoreCase(String userName);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByPhoneNumber(String phoneNumber);

	Optional<TenantAdminEntity> findByUserNameIgnoreCase(String userName);

	Optional<TenantAdminEntity> findByEmailIgnoreCase(String email);
}
