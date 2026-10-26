package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CashPaymentRegressionTest {
    @Test
    void confirmsCashAndActivatesQuotedDurationOnlyOnce() {
        MembershipRepository memberships = mock(MembershipRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        UserRepository users = mock(UserRepository.class);
        MembershipPaymentService service = new MembershipPaymentService(users,
                mock(BranchRepository.class), mock(MembershipPlanRepository.class), memberships, payments, notifications);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", mock(jakarta.persistence.EntityManager.class));
        User member = User.builder().id(1L).role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build();
        when(users.lockByIdForBooking(1L)).thenReturn(Optional.of(member));
        when(payments.existsByMembership_IdAndStatus(2L, PaymentStatus.PAID)).thenAnswer(invocation -> paymentPaid(payments));
        Membership membership = Membership.builder().id(2L).user(member).status(MembershipStatus.PENDING)
                .quotedDurationDays(30).branch(Branch.builder().active(true).build())
                .plan(MembershipPlan.builder().active(true).durationDays(90).build()).build();
        Payment payment = Payment.builder().id(3L).user(member).membership(membership)
                .amount(new BigDecimal("100000")).method(PaymentMethod.CASH).status(PaymentStatus.PENDING).build();
        when(payments.findById(3L)).thenReturn(Optional.of(payment));
        when(payments.lockById(3L)).thenReturn(Optional.of(payment));
        when(memberships.lockById(2L)).thenReturn(Optional.of(membership));
        service.confirmCashPayment(3L);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus());
        assertEquals(29, ChronoUnit.DAYS.between(membership.getStartDate(), membership.getEndDate()));
        assertThrows(IllegalArgumentException.class, () -> service.confirmCashPayment(3L));
        verify(memberships, times(1)).save(membership);
    }

    private boolean paymentPaid(PaymentRepository payments) {
        return payments.findById(3L).orElseThrow().getStatus() == PaymentStatus.PAID;
    }
}
