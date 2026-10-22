package com.utefit.service;

import com.utefit.enums.MembershipStatus;
import com.utefit.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** Đồng bộ trạng thái; nghiệp vụ vẫn phải kiểm tra endDate tại thời điểm sử dụng. */
@Service
@RequiredArgsConstructor
public class MembershipExpiryScheduler {
    private final MembershipRepository memberships;

    @Scheduled(cron = "0 5 * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void expireMemberships() {
        memberships.expireOutdated(MembershipStatus.ACTIVE,
                MembershipStatus.EXPIRED, LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
    }
}
