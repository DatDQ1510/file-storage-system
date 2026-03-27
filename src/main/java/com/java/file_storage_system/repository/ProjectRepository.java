package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends BaseRepository<ProjectEntity> {

	@Query("select p from ProjectEntity p where p.id = :projectId and p.tenant.id = :tenantId")
	Optional<ProjectEntity> findByIdAndTenantId(@Param("projectId") String projectId, @Param("tenantId") String tenantId);

	@Query("select (count(p) > 0) from ProjectEntity p where p.nameProject = :nameProject and p.tenant.id = :tenantId")
	boolean existsByNameProjectAndTenantId(@Param("nameProject") String nameProject, @Param("tenantId") String tenantId);

	@Query("""
			select p
			from ProjectEntity p
			where p.tenantAdmin.id = :tenantAdminId
			""")
	Page<ProjectEntity> findAllByTenantAdminId(
			@Param("tenantAdminId") String tenantAdminId,
			Pageable pageable
	);

	@Query("""
			select p
			from ProjectEntity p
			where p.tenantAdmin.id = :tenantAdminId
			and (
				:keyword is null
				or lower(p.nameProject) like lower(concat('%', :keyword, '%'))
			)
			""")
	Page<ProjectEntity> searchByTenantAdminIdAndKeyword(
			@Param("tenantAdminId") String tenantAdminId,
			@Param("keyword") String keyword,
			Pageable pageable
	);
}
