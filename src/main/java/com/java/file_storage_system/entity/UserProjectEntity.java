package com.java.file_storage_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "userProjects",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_project", columnNames = {"userId", "projectId"})
        }
)
public class UserProjectEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projectId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProjectEntity project;

    @Column(
            name = "permission",
            nullable = false,
            columnDefinition = "integer default 1",
            comment = "Bitmask permission: 1=READ, 2=WRITE, 4=DELETE, 8=MANAGE_MEMBER"
    )
    private Integer permission = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantedByUserId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity grantedByUser;
}
