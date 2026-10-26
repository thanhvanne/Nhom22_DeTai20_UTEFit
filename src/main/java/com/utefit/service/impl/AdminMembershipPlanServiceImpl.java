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
    private final com.utefit.repository.BranchRepository branches;

    @Override
    @Transactional(readOnly = true)
    public Page<MembershipPlan> getPlans(String keyword, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "durationDays"));
        String searchPattern = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        Page<MembershipPlan> result = planRepository.searchPlans(searchPattern, active, pageable);
        result.forEach(plan -> plan.getBranchIds().size());
        return result;
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
                .durationUnit(p.term().unit()).durationQuantity(p.term().quantity())
                .bonusUnit(p.term().bonusUnit()).bonusQuantity(p.term().bonusQuantity())
                .benefit(p.effectiveBenefit()).accessScope(p.effectiveScope()).branchIds(new java.util.HashSet<>(p.getBranchIds()))
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
                .durationUnit(dto.getDurationUnit()).durationQuantity(dto.getDurationQuantity())
                .bonusUnit(dto.getBonusUnit()).bonusQuantity(dto.getBonusQuantity())
                .benefit(dto.getBenefit()).accessScope(dto.getAccessScope()).branchIds(scopeBranches(dto))
                .name(dto.getName().trim())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .durationDays(term(dto).daysAt(java.time.LocalDate.of(2000, 1, 1)))
                .price(dto.getPrice())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        planRepository.save(plan);
    }

    @Override
    @Transactional
    public void updatePlan(Long id, MembershipPlanFormDto dto) {
        MembershipPlan plan = planRepository.lockForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập."));
        validatePlanBusinessRules(dto, id);
        plan.setBenefit(dto.getBenefit());
        plan.setDurationUnit(dto.getDurationUnit());
        plan.setDurationQuantity(dto.getDurationQuantity());
        plan.setBonusUnit(dto.getBonusUnit());
        plan.setBonusQuantity(dto.getBonusQuantity());
        plan.setAccessScope(dto.getAccessScope());
        plan.getBranchIds().clear();
        plan.getBranchIds().addAll(scopeBranches(dto));

        plan.setName(dto.getName().trim());
        plan.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        plan.setDurationDays(term(dto).daysAt(java.time.LocalDate.of(2000, 1, 1)));
        plan.setPrice(dto.getPrice());
        plan.setActive(dto.getActive() != null ? dto.getActive() : true);

        planRepository.save(plan);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        MembershipPlan plan = planRepository.lockForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập."));
        plan.setActive(!Boolean.TRUE.equals(plan.getActive()));
        planRepository.save(plan);
    }

    // Business Rules cốt lõi Giai đoạn 3: price > 0, duration > 0, chống trùng tên
    private void validatePlanBusinessRules(MembershipPlanFormDto dto, Long currentId) {
        if (dto.getBenefit() == null || dto.getAccessScope() == null || dto.getBranchIds() == null)
            throw new IllegalArgumentException("Quyền lợi/phạm vi chi nhánh không hợp lệ.");
        if (dto.getAccessScope() != com.utefit.enums.MembershipScope.SINGLE_BRANCH)
            throw new IllegalArgumentException("Gói mới/chỉnh sửa phải thuộc đúng một chi nhánh cố định. Quyền đã mua trước đây không thay đổi.");
        if (dto.getAccessScope() == com.utefit.enums.MembershipScope.SINGLE_BRANCH && dto.getBranchIds().size() != 1)
            throw new IllegalArgumentException("Gói SINGLE_BRANCH phải chọn đúng một chi nhánh đang hoạt động.");
        if (dto.getAccessScope() != com.utefit.enums.MembershipScope.ALL_BRANCHES) {
            if (dto.getBranchIds().stream().anyMatch(id -> id == null || id <= 0))
                throw new IllegalArgumentException("Mã chi nhánh không hợp lệ.");
            var selected = branches.findAllById(dto.getBranchIds());
            if (selected.size() != dto.getBranchIds().size() || selected.stream().anyMatch(branch -> !Boolean.TRUE.equals(branch.getActive())))
                throw new IllegalArgumentException("Phạm vi có chi nhánh không tồn tại hoặc ngừng hoạt động.");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá gói tập phải lớn hơn 0!");
        }
        term(dto);

        String name = dto.getName().trim();
        boolean duplicate = (currentId == null)
                ? planRepository.existsByNameIgnoreCase(name)
                : planRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);

        if (duplicate) {
            throw new IllegalArgumentException("Tên gói tập '" + name + "' đã tồn tại trong hệ thống!");
        }
    }

    private java.util.Set<Long> scopeBranches(MembershipPlanFormDto dto) {
        return new java.util.HashSet<>(dto.getBranchIds());
    }

    private com.utefit.util.MembershipTerm term(MembershipPlanFormDto dto) {
        if (dto.getDurationQuantity() == null || dto.getBonusQuantity() == null)
            throw new IllegalArgumentException("Cần nhập số lượng thời hạn và thời gian thưởng.");
        return new com.utefit.util.MembershipTerm(dto.getDurationQuantity(), dto.getDurationUnit(), dto.getBonusQuantity(), dto.getBonusUnit());
    }
}
