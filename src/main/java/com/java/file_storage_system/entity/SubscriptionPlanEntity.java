package com.java.file_storage_system.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties.Json;

import java.math.BigInteger;

@Data
@EqualsAndHashCode(callSuper = true)
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

    @Column(name = "features", nullable = true, columnDefinition = "TEXT",
            comment = "JSON string to store additional features and limits")
    private Json features; // JSON string to store additional features and limits
}
