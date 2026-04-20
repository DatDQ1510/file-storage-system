package com.java.file_storage_system.repository;

import com.java.file_storage_system.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<UserEntity> {

	@Query("select count(u) from UserEntity u where u.tenant.id = :tenantId")
	long countByTenantId(@Param("tenantId") String tenantId);

	@Query("""
			select u
			from UserEntity u
			where u.tenant.id = :tenantId
			order by u.createdAt desc
			""")
	Page<UserEntity> findAllByTenantId(
			@Param("tenantId") String tenantId,
			Pageable pageable
	);

	@Query("select (count(u) > 0) from UserEntity u where lower(u.email) = lower(:email)")
	boolean existsByEmailIgnoreCase(@Param("email") String email);

	Optional<UserEntity> findByEmailIgnoreCase(String email);

	Optional<UserEntity> findByUserNameIgnoreCase(String userName);

	@Query("select (count(u) > 0) from UserEntity u where lower(u.userName) = lower(:userName) and u.tenant.id = :tenantId")
	boolean existsByUserNameIgnoreCaseAndTenantId(@Param("userName") String userName, @Param("tenantId") String tenantId);

	@Query("""
			select u
			from UserEntity u
			where u.tenant.id = :tenantId
			and (
				lower(u.userName) like lower(concat('%', coalesce(:keyword, ''), '%'))
				or lower(u.email) like lower(concat('%', coalesce(:keyword, ''), '%'))
			)
			""")
	Page<UserEntity> searchByTenantIdAndKeyword(
			@Param("tenantId") String tenantId,
			@Param("keyword") String keyword,
			Pageable pageable
	);
}
