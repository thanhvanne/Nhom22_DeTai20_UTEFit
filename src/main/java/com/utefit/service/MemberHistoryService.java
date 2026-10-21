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

    public record History(
            List<MembershipPaymentService.MembershipView> memberships,
            List<MembershipPaymentService.PaymentView> payments,
            List<MemberClassBookingService.BookingView> classes,
            List<MemberReviewService.ReviewView> reviews
    ) {}

    @Transactional(readOnly = true)
    public History getHistory(String email) {
        return new History(
                billing.myMemberships(email),
                billing.myPayments(email),
                booking.mySchedule(email),
                reviews.myReviews(email, 0).getContent()
        );
    }
}