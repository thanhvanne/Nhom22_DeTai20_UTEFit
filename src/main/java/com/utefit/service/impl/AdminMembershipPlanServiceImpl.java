package com.utefit.service.impl;

import com.utefit.dto.admin.MembershipPlanFormDto;
import com.utefit.entity.MembershipPlan;
import com.utefit.repository.MembershipPlanRepository;
import com.utefit.service.AdminMembershipPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminMembershipPlanServiceImpl implements AdminMembershipPlanService {

    private final MembershipPlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<MembershipPlan> getPlans(String keyword, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "durationDays"));
        String searchPattern = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        return planRepository.searchPlans(searchPattern, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPlan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPlanFormDto getPlanFormById(Long id) {
        MembershipPlan p = getPlanById(id);
        return MembershipPlanFormDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .durationDays(p.getDurationDays())
                .price(p.getPrice())
                .active(p.getActive())
                .build();
    }

    @Override
    @Transactional
    public void createPlan(MembershipPlanFormDto dto) {
        validatePlanBusinessRules(dto, null);

        MembershipPlan plan = MembershipPlan.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .durationDays(dto.getDurationDays())
                .price(dto.getPrice())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        planRepository.save(plan);
    }

    @Override
    @Transactional
    public void updatePlan(Long id, MembershipPlanFormDto dto) {
        MembershipPlan plan = getPlanById(id);
        validatePlanBusinessRules(dto, id);

        plan.setName(dto.getName().trim());
        plan.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        plan.setDurationDays(dto.getDurationDays());
        plan.setPrice(dto.getPrice());
        plan.setActive(dto.getActive() != null ? dto.getActive() : true);

        planRepository.save(plan);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        MembershipPlan plan = getPlanById(id);
        plan.setActive(!Boolean.TRUE.equals(plan.getActive()));
        planRepository.save(plan);
    }

    // Business Rules cốt lõi Giai đoạn 3: price > 0, duration > 0, chống trùng tên
    private void validatePlanBusinessRules(MembershipPlanFormDto dto, Long currentId) {
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá gói tập phải lớn hơn 0!");
        }
        if (dto.getDurationDays() == null || dto.getDurationDays() <= 0) {
            throw new IllegalArgumentException("Thời hạn gói tập (số ngày) phải lớn hơn 0!");
        }

        String name = dto.getName().trim();
        boolean duplicate = (currentId == null)
                ? planRepository.existsByNameIgnoreCase(name)
                : planRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);

        if (duplicate) {
            throw new IllegalArgumentException("Tên gói tập '" + name + "' đã tồn tại trong hệ thống!");
        }
    }
}