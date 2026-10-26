package com.utefit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlan {
    @Enumerated(EnumType.STRING) @Column(name = "duration_unit", length = 10)
    private com.utefit.enums.MembershipDurationUnit durationUnit;
    @Column(name = "duration_quantity") private Integer durationQuantity;
    @Enumerated(EnumType.STRING) @Column(name = "bonus_unit", length = 10)
    private com.utefit.enums.MembershipDurationUnit bonusUnit;
    @Column(name = "bonus_quantity") private Integer bonusQuantity;

    public com.utefit.util.MembershipTerm term() {
        return durationUnit == null ? com.utefit.util.MembershipTerm.legacy(durationDays)
                : new com.utefit.util.MembershipTerm(durationQuantity == null ? 0 : durationQuantity, durationUnit,
                bonusQuantity == null ? 0 : bonusQuantity, bonusUnit);
    }

    public boolean fixedBranch() { return effectiveScope() == com.utefit.enums.MembershipScope.SINGLE_BRANCH && branchIds.size() == 1; }
    public String durationDescription() { return term().description(); }
    @Enumerated(EnumType.STRING) @Column(name = "benefit", length = 20)
    @Builder.Default private com.utefit.enums.MembershipBenefit benefit = com.utefit.enums.MembershipBenefit.ALL;
    @Enumerated(EnumType.STRING) @Column(name = "access_scope", length = 20)
    @Builder.Default private com.utefit.enums.MembershipScope accessScope = com.utefit.enums.MembershipScope.SINGLE_BRANCH;
    @ElementCollection @CollectionTable(name = "membership_plan_branches", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "branch_id", nullable = false)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default private java.util.Set<Long> branchIds = new java.util.HashSet<>();

    public com.utefit.enums.MembershipBenefit effectiveBenefit() { return benefit == null ? com.utefit.enums.MembershipBenefit.ALL : benefit; }
    public com.utefit.enums.MembershipScope effectiveScope() { return accessScope == null ? com.utefit.enums.MembershipScope.SINGLE_BRANCH : accessScope; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 100)
    private String name;

    @org.hibernate.annotations.Nationalized
    @Column(length = 1500)
    private String description;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
