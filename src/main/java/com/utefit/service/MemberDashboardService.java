package com.utefit.service;

import com.utefit.dto.notification.NotificationResponse;
import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;
import com.utefit.dto.trainer.TrainerResponse;
import com.utefit.dto.trainer.TrainerSearchRequest;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberDashboardService {

    private static final ZoneId GYM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final MemberAccountService accounts;
    private final UserRepository users;
    private final MembershipPaymentService billing;
    private final MemberClassBookingService classBookings;
    private final MemberPersonalScheduleService ptSchedules;
    private final MemberNotificationService notifications;
    private final PublicCatalogService catalog;
    private final TrainerService trainers;

    @Transactional(readOnly = true)
    public DashboardView getDashboard(String email) {

        Long userId = accounts.requireMemberId(email);

        User user = users.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Không tìm thấy hội viên."
                        )
                );

        LocalDate today = LocalDate.now(GYM_ZONE);
        LocalDateTime now = LocalDateTime.now(GYM_ZONE);

        // Gói tập đang có hiệu lực thực sự.
        List<MembershipPaymentService.MembershipView> memberships =
                billing.myMemberships(email);

        MembershipPaymentService.MembershipView active =
                memberships.stream()
                        .filter(m -> "ACTIVE".equals(m.status()))
                        .filter(m ->
                                m.startDate() != null
                                        && m.endDate() != null
                        )
                        .filter(m ->
                                !m.startDate().isAfter(today)
                                        && !m.endDate().isBefore(today)
                        )
                        .findFirst()
                        .orElse(null);

        boolean pending = memberships.stream()
                .anyMatch(m -> "PENDING".equals(m.status()));

        // Giao dịch mới nhất.
        List<MembershipPaymentService.PaymentView> payments =
                billing.myPayments(email);

        MembershipPaymentService.PaymentView latestPayment =
                payments.isEmpty()
                        ? null
                        : payments.get(0);

        // Kết hợp lịch lớp nhóm và lịch PT cá nhân.
        List<UpcomingSession> upcoming = new ArrayList<>();

        for (MemberClassBookingService.BookingView b
                : classBookings.mySchedule(email)) {

            if ("REGISTERED".equals(b.status())
                    && b.date() != null
                    && b.startTime() != null
                    && LocalDateTime.of(
                    b.date(),
                    b.startTime()
            ).isAfter(now)) {

                upcoming.add(new UpcomingSession(
                        "CLASS",
                        b.className(),
                        b.branchName(),
                        b.date(),
                        b.startTime(),
                        b.endTime()
                ));
            }
        }

        for (PersonalTrainingScheduleResponse s
                : ptSchedules.getMySchedules(
                email,
                today,
                today.plusMonths(6)
        )) {

            if ("SCHEDULED".equals(s.getStatus())
                    && s.getScheduleDate() != null
                    && s.getStartTime() != null
                    && LocalDateTime.of(
                    s.getScheduleDate(),
                    s.getStartTime()
            ).isAfter(now)) {

                String title =
                        (s.getTitle() == null
                                || s.getTitle().isBlank())
                                ? "Buổi tập PT 1:1"
                                : s.getTitle();

                upcoming.add(new UpcomingSession(
                        "PT",
                        title + " · " + s.getTrainerName(),
                        s.getBranchName(),
                        s.getScheduleDate(),
                        s.getStartTime(),
                        s.getEndTime()
                ));
            }
        }

        upcoming.sort(
                Comparator.comparing(UpcomingSession::date)
                        .thenComparing(UpcomingSession::startTime)
        );

        // PT công khai, không hard-code hồ sơ.
        TrainerSearchRequest trainerSearch =
                new TrainerSearchRequest();

        List<TrainerResponse> featuredTrainers =
                trainers.searchPublicTrainers(trainerSearch)
                        .getContent()
                        .stream()
                        .limit(2)
                        .toList();

        // Thông báo thuộc Member đang đăng nhập.
        List<NotificationResponse> recentNotifications =
                notifications.getMyNotifications(email)
                        .stream()
                        .limit(3)
                        .toList();

        // Tiến độ sử dụng gói, tính từ ngày thực tế.
        long daysLeft = active == null
                ? 0
                : ChronoUnit.DAYS.between(
                today,
                active.endDate()
        ) + 1;

        int progress = 0;

        if (active != null) {

            long total = ChronoUnit.DAYS.between(
                    active.startDate(),
                    active.endDate()
            ) + 1;

            long elapsed = ChronoUnit.DAYS.between(
                    active.startDate(),
                    today
            ) + 1;

            if (total > 0) {
                progress = (int) Math.max(
                        0,
                        Math.min(100, elapsed * 100 / total)
                );
            }
        }

        return new DashboardView(
                user.getFullName(),
                active,
                pending,
                daysLeft,
                progress,
                latestPayment,
                upcoming.stream().limit(4).toList(),
                catalog.featuredClasses(),
                featuredTrainers,
                recentNotifications,
                notifications.countUnread(email)
        );
    }

    public record UpcomingSession(
            String kind,
            String title,
            String branchName,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {}

    public record DashboardView(
            String memberName,
            MembershipPaymentService.MembershipView activeMembership,
            boolean pendingMembership,
            long daysLeft,
            int progressPercent,
            MembershipPaymentService.PaymentView latestPayment,
            List<UpcomingSession> upcoming,
            List<PublicCatalogService.ClassView> featuredClasses,
            List<TrainerResponse> featuredTrainers,
            List<NotificationResponse> notifications,
            long unreadCount
    ) {}
}