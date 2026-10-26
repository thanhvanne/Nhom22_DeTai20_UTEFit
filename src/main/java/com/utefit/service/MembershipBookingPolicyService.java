package com.utefit.service;

import com.utefit.enums.*;
import com.utefit.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
@RequiredArgsConstructor
public class MembershipBookingPolicyService {
    private final UserRepository users;
    private final ClassRegistrationRepository registrations;
    private final MemberClassScheduleRepository schedules;
    private final PersonalTrainingScheduleRepository personal;
    private final MembershipEligibilityService eligibility;
    private final NotificationService notifications;
    private final EntityManager entityManager;

    @Transactional(timeout = 30)
    public int revalidateMember(Long memberId) {
        users.lockByIdForBooking(memberId).orElseThrow();
        LocalDateTime now = LocalDateTime.now(SepayPaymentService.ZONE);
        int cancelled = 0;
        for (var registration : registrations.findFutureForMember(memberId, now.toLocalDate())) {
            var schedule = schedules.lockById(registration.getSchedule().getId()).orElseThrow();
            entityManager.refresh(registration);
            if (registration.getStatus() != RegistrationStatus.REGISTERED
                    || !LocalDateTime.of(schedule.getScheduleDate(), schedule.getStartTime()).isAfter(now)) continue;
            var fitnessClass = schedule.getFitnessClass();
            if (!eligibility.canUse(memberId, fitnessClass.getBranch().getId(), schedule.getScheduleDate(), fitnessClass.getCategory().getRequiredBenefit())) {
                registration.setStatus(RegistrationStatus.CANCELLED);
                notifications.createNotificationForUser(memberId, NotificationType.CLASS_SCHEDULE_CANCELLED,
                        "Đã hủy giữ chỗ do quyền lợi không còn hợp lệ",
                        "Lớp " + fitnessClass.getName() + " ngày " + schedule.getScheduleDate()
                                + " không còn được Membership đã thanh toán bao phủ. Khoản gia hạn bị hủy/hoàn tiền không còn giữ chỗ.", schedule.getId());
                cancelled++;
            }
        }
        for (var session : personal.findFutureForMember(memberId, now.toLocalDate())) {
            entityManager.refresh(session, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (!Boolean.TRUE.equals(session.getActive()) || (session.getStatus() != null && session.getStatus() != PersonalTrainingStatus.SCHEDULED)
                    || !LocalDateTime.of(session.getScheduleDate(), session.getStartTime()).isAfter(now)) continue;
            Long branchId = session.getBranch() == null ? session.getTrainer().getBranch().getId() : session.getBranch().getId();
            if (!eligibility.canUse(memberId, branchId, session.getScheduleDate(), MembershipBenefit.PT)) {
                session.setActive(false);
                session.setStatus(PersonalTrainingStatus.CANCELLED);
                notifications.createNotificationForUser(memberId, NotificationType.PERSONAL_SCHEDULE_CANCELLED,
                        "Đã hủy lịch PT do quyền lợi không còn hợp lệ", "Lịch PT ngày " + session.getScheduleDate()
                                + " không còn được gói đã thanh toán bao phủ.", session.getId());
                notifications.createNotificationForUser(session.getTrainer().getUser().getId(), NotificationType.PERSONAL_SCHEDULE_CANCELLED,
                        "Lịch PT bị hủy do quyền lợi hội viên", "Hội viên không còn quyền lợi tại ngày tập " + session.getScheduleDate(), session.getId());
                cancelled++;
            }
        }
        return cancelled;
    }
}
