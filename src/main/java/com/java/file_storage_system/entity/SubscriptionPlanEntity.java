package com.java.file_storage_system.entity;


import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;

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

    @Column(name = "billingCycle", nullable = false, comment = "Billing cycle for the subscription plan")
    private String billingCycle; // e.g., "Monthly", "Yearly", "Quarterly"

    @Column(name = "features", columnDefinition = "jsonb",
            comment = "JSON object to store additional features and limits")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode features; // JSON object to store additional features and limits
}
