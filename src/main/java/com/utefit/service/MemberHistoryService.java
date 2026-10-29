package com.utefit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberHistoryService {

    private final MembershipPaymentService billing;
    private final MemberClassBookingService booking;
    private final MemberReviewService reviews;
    private final MemberAccountService accounts;
    private final com.utefit.repository.CheckinRepository checkins;
    private final PersonalTrainingScheduleService personalSchedules;

    public record History(
            List<MembershipPaymentService.MembershipView> memberships,
            List<MembershipPaymentService.PaymentView> payments,
            List<MemberClassBookingService.BookingView> classes,
            List<MemberReviewService.ReviewView> reviews,
            List<CheckinHistory> checkins,
            List<com.utefit.dto.trainer.PersonalTrainingScheduleResponse> personalSchedules
    ) {}

    public record CheckinHistory(String branch, java.time.LocalDateTime inAt,
                                 java.time.LocalDateTime outAt) {}

    @Transactional(readOnly = true)
    public History getHistory(String email) {
        return new History(
                billing.myMemberships(email),
                billing.myPayments(email),
                booking.mySchedule(email),
                reviews.allHistory(email),
                checkins.findByUser_IdOrderByCheckInAtDesc(accounts.requireMemberId(email)).stream()
                        .map(c -> new CheckinHistory(c.getBranch().getName(), c.getCheckInAt(), c.getCheckOutAt()))
                        .toList(),
                personalSchedules.getSchedulesForMember(accounts.requireMemberId(email),
                        java.time.LocalDate.of(1900, 1, 1), java.time.LocalDate.of(9999, 12, 31))
        );
    }
}
