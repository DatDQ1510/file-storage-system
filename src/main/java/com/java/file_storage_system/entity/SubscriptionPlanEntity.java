package com.java.file_storage_system.entity;


import com.fasterxml.jackson.databind.JsonNode;
import com.java.file_storage_system.constant.BillingCycle;
import com.java.file_storage_system.constant.PlanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.math.BigInteger;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "subscriptionPlans")
public class SubscriptionPlanEntity extends BaseEntity {

    @Column(name = "namePlan", nullable = false, unique = true)
    private String namePlan;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "baseStorageLimit", nullable = false, comment = "Base storage limit in GB")
    private BigInteger baseStorageLimit; // in GB

    @Column(name = "maxUsers", nullable = false, comment = "Maximum number of users allowed under this plan")
    private Integer maxUsers;

    @Column(name = "price", nullable = false, comment = "Price of the subscription plan in USD")
    private Double price; // in USD

    @Enumerated(EnumType.STRING)
    @Column(name = "billingCycle", nullable = false, comment = "MONTHLY, QUARTERLY, YEARLY")
    private BillingCycle billingCycle;

    @Column(name = "features", columnDefinition = "jsonb",
            comment = "JSON object to store additional features and limits")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode features; // JSON object to store additional features and limits

    @Enumerated(EnumType.STRING)
    @Column(name = "planStatus")
    private PlanStatus planStatus;

    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<TenantPlan> tenantPlans = new ArrayList<>();

}
