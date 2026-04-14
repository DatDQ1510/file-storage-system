package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.TenantStatus;
import com.java.file_storage_system.constant.TenantPlanStatus;
import com.java.file_storage_system.dto.tenant.AllTenantPageResponse;
import com.java.file_storage_system.dto.tenant.AllTenantResponse;
import com.java.file_storage_system.dto.tenant.CreateTenantRequest;
import com.java.file_storage_system.dto.tenant.UpdateTenantRequest;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class TenantServiceImpl extends BaseServiceImpl<TenantEntity, TenantRepository> implements TenantService {

    private final TenantRepository tenantRepository;

    public TenantServiceImpl(TenantRepository repository) {
        super(repository);
        this.tenantRepository = repository;
    }

    @Override
    public TenantEntity createTenant(CreateTenantRequest request) {
        String normalizedName = normalize(request.getNameTenant());
        String normalizedDomain = normalize(request.getDomainTenant());

        if (tenantRepository.existsByDomainTenant(normalizedDomain)) {
            throw ConflictException.alreadyExists("Tenant", "domainTenant", normalizedDomain);
        }

        TenantEntity tenant = new TenantEntity();
        tenant.setNameTenant(normalizedName);
        tenant.setDomainTenant(normalizedDomain);
        tenant.setExTraStorageSize(request.getExTraStorageSize());
        tenant.setUsedStorageSize(BigInteger.ZERO);
        tenant.setStatusTenant(request.getStatusTenant() == null ? TenantStatus.ACTIVE : request.getStatusTenant());

        return tenantRepository.save(tenant);
    }

    @Override
    public TenantEntity updateTenant(String tenantId, UpdateTenantRequest request) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.byField("Tenant", "id", tenantId));

        String normalizedName = normalize(request.getNameTenant());
        String normalizedDomain = normalize(request.getDomainTenant());

        if (tenantRepository.existsByDomainTenantAndIdNot(normalizedDomain, tenantId)) {
            throw ConflictException.alreadyExists("Tenant", "domainTenant", normalizedDomain);
        }

        tenant.setNameTenant(normalizedName);
        tenant.setDomainTenant(normalizedDomain);
        tenant.setExTraStorageSize(request.getExTraStorageSize());
        tenant.setUsedStorageSize(request.getUsedStorageSize());
        tenant.setStatusTenant(request.getStatusTenant());

        return tenantRepository.save(tenant);
    }

    @Override
    public AllTenantPageResponse getAllTenants(int page, int offset) {
        int normalizedPage = Math.max(0, page);
        int normalizedOffset = Math.max(1, offset);

        log.info(
            "Loading tenants with pagination input page={}, offset={} normalized to page={}, offset={}",
            page,
            offset,
            normalizedPage,
            normalizedOffset
        );

        Page<TenantRepository.TenantSummaryProjection> tenantPage = tenantRepository.findAllTenantSummaries(
            PageRequest.of(normalizedPage, normalizedOffset)
        );

        List<AllTenantResponse> items = tenantPage.getContent().stream()
            .map(this::mapToAllTenantResponse)
            .toList();

        log.info(
            "Loaded tenant page successfully: page={}, offset={}, totalElements={}, totalPages={}, hasNext={}, hasPrevious={}",
            tenantPage.getNumber(),
            tenantPage.getSize(),
            tenantPage.getTotalElements(),
            tenantPage.getTotalPages(),
            tenantPage.hasNext(),
            tenantPage.hasPrevious()
        );

        return new AllTenantPageResponse(
            items,
            tenantPage.getNumber(),
            tenantPage.getSize(),
            tenantPage.getTotalElements(),
            tenantPage.getTotalPages(),
            tenantPage.hasNext(),
            tenantPage.hasPrevious()
        );
    }

    @Override
    public boolean existsByDomainTenant(String domainTenant) {
        return tenantRepository.existsByDomainTenant(normalize(domainTenant));
    }

    private AllTenantResponse mapToAllTenantResponse(TenantRepository.TenantSummaryProjection projection) {
        TenantStatus statusTenant = projection.getStatusTenant();

        BillingCycle planBillingCycle = projection.getPlanBillingCycle();

        TenantPlanStatus tenantPlanStatus = projection.getTenantPlanStatus();

        return new AllTenantResponse(
            projection.getId(),
            projection.getNameTenant(),
            projection.getDomainTenant(),
            projection.getExTraStorageSize(),
            projection.getUsedStorageSize(),
            statusTenant,
            projection.getTenantAdminId(),
            projection.getTenantAdminUserName(),
            projection.getTenantAdminEmail(),
            projection.getTenantAdminPhoneNumber(),
            projection.getPlanId(),
            projection.getPlanName(),
            projection.getPlanBaseStorageLimit(),
            projection.getPlanPrice(),
            planBillingCycle,
            tenantPlanStatus,
            projection.getPlanStartDate(),
            projection.getPlanEndDate(),
            projection.getCreatedAt(),
            projection.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
