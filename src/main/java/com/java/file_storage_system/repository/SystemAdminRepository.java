package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.SystemAdminEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemAdminRepository extends BaseRepository<SystemAdminEntity> {

	boolean existsByUserNameIgnoreCase(String username);

	Optional<SystemAdminEntity> findByUserNameIgnoreCase(String username);
}
