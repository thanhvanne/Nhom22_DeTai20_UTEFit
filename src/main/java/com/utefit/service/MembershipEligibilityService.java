package com.utefit.service;

import com.utefit.entity.Membership;
import com.utefit.enums.MembershipStatus;
import com.utefit.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MembershipEligibilityService {
    private final MembershipRepository memberships;
    private final com.utefit.repository.BranchRepository branches;

    @Transactional(readOnly = true)
    public boolean canUseBranch(Long memberId, Long branchId, LocalDate day) {
        return canUse(memberId, branchId, day, null);
    }

    @Transactional(readOnly = true)
    public boolean canUse(Long memberId, Long branchId, LocalDate day, com.utefit.enums.MembershipBenefit benefit) {
        if (memberId == null || branchId == null || day == null) return false;
        if (branches.findById(branchId).filter(branch -> Boolean.TRUE.equals(branch.getActive())).isEmpty()) return false;
        return memberships.findPaidAccessByUser(memberId).stream().anyMatch(m -> covers(m, branchId, day, benefit));
    }

    public static boolean covers(Membership membership, Long branchId, LocalDate day, com.utefit.enums.MembershipBenefit benefit) {
        return membership.getStatus() == MembershipStatus.ACTIVE && allowsBranch(membership, branchId)
                && allowsBenefit(membership, benefit)
                && membership.getStartDate() != null && !membership.getStartDate().isAfter(day)
                && membership.getEndDate() != null && !membership.getEndDate().isBefore(day);
    }

    public static boolean allowsBranch(Membership membership, Long branchId) {
        return switch (membership.effectiveScope()) {
            case SINGLE_BRANCH -> membership.getBranch().getId().equals(branchId);
            case MULTI_BRANCH -> membership.getBranchIds().contains(branchId);
            case ALL_BRANCHES -> true;
        };
    }

    private static boolean allowsBenefit(Membership membership, com.utefit.enums.MembershipBenefit benefit) {
        return benefit == null || benefit == com.utefit.enums.MembershipBenefit.ALL
                || membership.effectiveBenefit() == com.utefit.enums.MembershipBenefit.ALL || membership.effectiveBenefit() == benefit;
    }

    public static String accessDenial(java.util.List<Membership> paid, Long branchId, LocalDate day, com.utefit.enums.MembershipBenefit benefit) {
        if (paid.isEmpty()) return "Chưa có kỳ Membership đã thanh toán hợp lệ. PENDING/REVIEW/FAILED không giữ chỗ.";
        var branchAccess = paid.stream().filter(membership -> allowsBranch(membership, branchId)).toList();
        if (branchAccess.isEmpty()) return "Phạm vi các gói đã thanh toán không bao phủ chi nhánh của lớp.";
        var rights = branchAccess.stream().filter(membership -> allowsBenefit(membership, benefit)).toList();
        if (rights.isEmpty()) return "Thiếu quyền lợi " + benefit + " hoặc ALL tại chi nhánh của lớp.";
        return "Chưa có kỳ đã thanh toán bao phủ ngày học " + day
                + ". Hạn đã mua tương ứng: " + rights.stream().map(Membership::getEndDate).filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo).map(LocalDate::toString).orElse("chưa có")
                + ". Cần thanh toán gia hạn, không thể dùng kỳ PENDING để giữ chỗ.";
    }

    public void requireAccess(Long memberId, Long branchId, LocalDate day) {
        if (!canUseBranch(memberId, branchId, day)) {
            throw new IllegalStateException(
                    "Cần Membership ACTIVE và còn hạn tại chi nhánh vào ngày sử dụng.");
        }
    }

    @Transactional(readOnly = true)
    public void requirePersonalTraining(Long memberId, Long branchId, LocalDate day) {
        if (!canUse(memberId, branchId, day, com.utefit.enums.MembershipBenefit.PT))
            throw new IllegalStateException("Cần quyền lợi PT đã thanh toán, đúng chi nhánh và ngày tập.");
    }
}
