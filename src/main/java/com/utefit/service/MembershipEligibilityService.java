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

    @Transactional(readOnly = true)
    public boolean canUseBranch(Long memberId, Long branchId, LocalDate day) {
        if (memberId == null || branchId == null || day == null) return false;
        return memberships.findByUser_IdOrderByCreatedAtDesc(memberId).stream().anyMatch(m ->
                m.getStatus() == MembershipStatus.ACTIVE
                        && m.getBranch().getId().equals(branchId)
                        && m.getStartDate() != null && !m.getStartDate().isAfter(day)
                        && m.getEndDate() != null && !m.getEndDate().isBefore(day));
    }

    public void requireAccess(Long memberId, Long branchId, LocalDate day) {
        if (!canUseBranch(memberId, branchId, day)) {
            throw new IllegalStateException(
                    "Cần Membership ACTIVE và còn hạn tại chi nhánh vào ngày sử dụng.");
        }
    }
}
