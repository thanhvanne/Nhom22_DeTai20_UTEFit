package com.utefit.service;

import com.utefit.entity.Membership;
import com.utefit.enums.MembershipStatus;
import com.utefit.enums.PaymentStatus;
import com.utefit.repository.MembershipRepository;
import com.utefit.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Tự đóng yêu cầu mua gói chưa gửi yêu cầu thanh toán sau 48 giờ.
 * Không hủy đơn CASH đang chờ Admin, tránh tác động khoản tiền tại quầy. */
@Service
@RequiredArgsConstructor
public class PendingMembershipCleanupScheduler {
    @org.springframework.beans.factory.annotation.Autowired private PromotionReservationService promotionReservations;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final MembershipRepository memberships;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final PaymentRepository payments;

    @Scheduled(cron="0 15 * * * *",zone="Asia/Ho_Chi_Minh")
    @Transactional
    public void closeAbandonedMemberships() {
        LocalDateTime deadline=LocalDateTime.now(ZONE).minusHours(48);
        for (Membership old : memberships.findByStatusAndCreatedAtBefore(MembershipStatus.PENDING,deadline)) {
            // Khóa để không đua với xác nhận CASH.
            Membership m=memberships.lockById(old.getId()).orElse(null);
            if (m==null) continue;
            entityManager.refresh(m);
            if (m.getStatus()!=MembershipStatus.PENDING) continue;
            if (payments.existsByMembership_IdAndStatus(m.getId(),PaymentStatus.CANCEL_REQUESTED)
                    || payments.existsByMembership_IdAndStatus(m.getId(),PaymentStatus.PENDING)
                    || payments.existsByMembership_IdAndStatus(m.getId(),PaymentStatus.PAID)) continue;
            if (m.getPromotionCode() != null) promotionReservations.release(m);
            m.setStatus(MembershipStatus.CANCELLED);
        }
    }
}
