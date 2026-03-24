package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends BaseRepository<UserEntity> {

	@Query("select count(u) from UserEntity u where u.tenant.id = :tenantId")
	long countByTenantId(@Param("tenantId") String tenantId);

	boolean existsByEmail(String email);

	@Query("select (count(u) > 0) from UserEntity u where u.userName = :userName and u.tenant.id = :tenantId")
	boolean existsByUserNameAndTenantId(@Param("userName") String userName, @Param("tenantId") String tenantId);
}
