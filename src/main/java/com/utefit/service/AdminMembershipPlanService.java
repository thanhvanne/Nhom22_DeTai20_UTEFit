package com.utefit.service;

import com.utefit.dto.admin.MembershipPlanFormDto;
import com.utefit.entity.MembershipPlan;
import org.springframework.data.domain.Page;

public interface AdminMembershipPlanService {

    Page<MembershipPlan> getPlans(String keyword, Boolean active, int page, int size);

    MembershipPlan getPlanById(Long id);

    MembershipPlanFormDto getPlanFormById(Long id);

    void createPlan(MembershipPlanFormDto dto);

    void updatePlan(Long id, MembershipPlanFormDto dto);

    void toggleActive(Long id);
}