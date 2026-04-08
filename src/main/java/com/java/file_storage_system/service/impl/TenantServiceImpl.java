package com.java.file_storage_system.service.impl;

import com.java.file_storage_system.constant.TenantStatus;
import com.java.file_storage_system.dto.tenant.CreateTenantRequest;
import com.java.file_storage_system.dto.tenant.UpdateTenantRequest;
import com.java.file_storage_system.entity.TenantEntity;
import com.java.file_storage_system.exception.ConflictException;
import com.java.file_storage_system.exception.ResourceNotFoundException;
import com.java.file_storage_system.repository.TenantRepository;
import com.java.file_storage_system.service.TenantService;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

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
    public boolean existsByDomainTenant(String domainTenant) {
        return tenantRepository.existsByDomainTenant(normalize(domainTenant));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
