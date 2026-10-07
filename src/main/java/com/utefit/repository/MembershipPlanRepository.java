package com.utefit.repository;

import com.utefit.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanRepository
        extends JpaRepository<MembershipPlan, Long> {
}