package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.TenantPlan;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantPlanRepository extends BaseRepository<TenantPlan> {

	@Query("""
		select tp from TenantPlan tp
		where tp.tenant.id = :tenantId and tp.status = :status
		order by tp.createdAt desc
	""")
	Optional<TenantPlan> findLatestByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") String status);

	@Query("""
		select tp from TenantPlan tp
		where tp.tenant.id = :tenantId
		order by tp.createdAt desc
	""")
	Optional<TenantPlan> findLatestByTenantId(@Param("tenantId") String tenantId);
}
