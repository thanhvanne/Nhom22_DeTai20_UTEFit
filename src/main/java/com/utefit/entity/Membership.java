package com.utefit.entity;

import com.utefit.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {
    @Column(name = "promotion_code", length = 50, updatable = false)
    private String promotionCode;
    @Column(name = "original_price", precision = 15, scale = 2, updatable = false)
    private BigDecimal originalPrice;
    @Column(name = "discount_percent", precision = 5, scale = 2, updatable = false)
    private BigDecimal discountPercent;
    @Enumerated(EnumType.STRING) @Column(name = "quoted_duration_unit", length = 10, updatable = false)
    private com.utefit.enums.MembershipDurationUnit quotedDurationUnit;
    @Column(name = "quoted_duration_quantity", updatable = false) private Integer quotedDurationQuantity;
    @Enumerated(EnumType.STRING) @Column(name = "quoted_bonus_unit", length = 10, updatable = false)
    private com.utefit.enums.MembershipDurationUnit quotedBonusUnit;
    @Column(name = "quoted_bonus_quantity", updatable = false) private Integer quotedBonusQuantity;

    public com.utefit.util.MembershipTerm term() {
        return quotedDurationUnit == null ? com.utefit.util.MembershipTerm.legacy(quotedDurationDays != null ? quotedDurationDays : plan.getDurationDays())
                : new com.utefit.util.MembershipTerm(quotedDurationQuantity == null ? 0 : quotedDurationQuantity,
                quotedDurationUnit, quotedBonusQuantity == null ? 0 : quotedBonusQuantity, quotedBonusUnit);
    }
    @Enumerated(EnumType.STRING) @Column(name = "quoted_benefit", length = 20, updatable = false)
    private com.utefit.enums.MembershipBenefit quotedBenefit;
    @Enumerated(EnumType.STRING) @Column(name = "quoted_scope", length = 20, updatable = false)
    private com.utefit.enums.MembershipScope quotedScope;
    @ElementCollection @CollectionTable(name = "membership_branch_snapshot", joinColumns = @JoinColumn(name = "membership_id"))
    @Column(name = "branch_id", nullable = false)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default private java.util.Set<Long> branchIds = new java.util.HashSet<>();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "renewal_of_id")
    private Membership renewalOf;

    public com.utefit.enums.MembershipBenefit effectiveBenefit() { return quotedBenefit == null ? com.utefit.enums.MembershipBenefit.ALL : quotedBenefit; }
    public com.utefit.enums.MembershipScope effectiveScope() { return quotedScope == null ? com.utefit.enums.MembershipScope.SINGLE_BRANCH : quotedScope; }
    public String entitlementKey() {
        return effectiveBenefit() + ":" + switch (effectiveScope()) {
            case ALL_BRANCHES -> "*";
            case SINGLE_BRANCH -> java.util.Set.of(branch.getId()).toString();
            case MULTI_BRANCH -> new java.util.TreeSet<>(branchIds).toString();
        };
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membership_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membership_branch")
    )
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "plan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membership_plan")
    )
    private MembershipPlan plan;

    /** Giá và số ngày cam kết khi Member tạo đơn; nullable cho gói cũ. */
    @Column(name = "quoted_price", precision = 15, scale = 2)
    private BigDecimal quotedPrice;

    @Column(name = "quoted_duration_days")
    private Integer quotedDurationDays;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = MembershipStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
