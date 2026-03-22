package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Index;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "TenantPlan",
        indexes = {
                @Index(name = "idx_tenant_plan_id", columnList = "tenantId, planId")
        }
)
public class TenantPlan extends BaseEntity {

    @Column(name = "tenantId", nullable = false)
    private String tenantId;

    @Column(name = "planId", nullable = false)
    private String planId;

    @Column(name = "status")
    private String status;

    @Column(name = "planStartDate")
    private LocalDateTime planStartDate;

    @Column(name = "planEndDate")
    private LocalDateTime planEndDate;

    @Column(name = "isAutoRenew", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isAutoRenew;

}
