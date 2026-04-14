package com.java.file_storage_system.repository;

import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.TenantPlanStatus;
import com.java.file_storage_system.constant.TenantStatus;
import com.java.file_storage_system.entity.TenantPlan;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.entity.TenantAdminEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends BaseRepository<TenantEntity> {

	interface TenantSummaryProjection {
		String getId();

		String getNameTenant();

		String getDomainTenant();

		java.math.BigInteger getExTraStorageSize();

		java.math.BigInteger getUsedStorageSize();

		TenantStatus getStatusTenant();

		String getTenantAdminId();

		String getTenantAdminUserName();

		String getTenantAdminEmail();

		String getTenantAdminPhoneNumber();

		String getPlanId();

		String getPlanName();

		java.math.BigInteger getPlanBaseStorageLimit();

		Double getPlanPrice();

		BillingCycle getPlanBillingCycle();

		TenantPlanStatus getTenantPlanStatus();

		java.time.LocalDateTime getPlanStartDate();

		java.time.LocalDateTime getPlanEndDate();

		java.time.LocalDateTime getCreatedAt();

		java.time.LocalDateTime getUpdatedAt();
	}

	@Query(
		value = """
			SELECT
				t.id AS id,
				t.nameTenant AS nameTenant,
				t.domainTenant AS domainTenant,
				t.exTraStorageSize AS exTraStorageSize,
				t.usedStorageSize AS usedStorageSize,
				t.statusTenant AS statusTenant,
				ta.id AS tenantAdminId,
				ta.userName AS tenantAdminUserName,
				ta.email AS tenantAdminEmail,
				ta.phoneNumber AS tenantAdminPhoneNumber,
				sp.id AS planId,
				sp.namePlan AS planName,
				sp.baseStorageLimit AS planBaseStorageLimit,
				sp.price AS planPrice,
				sp.billingCycle AS planBillingCycle,
				tp.status AS tenantPlanStatus,
				tp.planStartDate AS planStartDate,
				tp.planEndDate AS planEndDate,
				t.createdAt AS createdAt,
				t.updatedAt AS updatedAt
			FROM TenantEntity t
			LEFT JOIN t.tenantAdmins ta ON ta.createdAt = (
				SELECT MAX(ta2.createdAt)
				FROM TenantAdminEntity ta2
				WHERE ta2.tenant = t
			)
			LEFT JOIN t.tenantPlans tp ON tp.createdAt = (
				SELECT MAX(tp2.createdAt)
				FROM TenantPlan tp2
				WHERE tp2.tenant = t
			)
			LEFT JOIN tp.plan sp
			ORDER BY t.createdAt DESC
		""",
		countQuery = "SELECT COUNT(t.id) FROM TenantEntity t"
	)
	Page<TenantSummaryProjection> findAllTenantSummaries(Pageable pageable);

	boolean existsByNameTenant(String nameTenant);

	boolean existsByDomainTenant(String domainTenant);

	boolean existsByNameTenantAndIdNot(String nameTenant, String id);

	boolean existsByDomainTenantAndIdNot(String domainTenant, String id);
}
