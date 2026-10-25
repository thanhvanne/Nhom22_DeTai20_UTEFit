package com.utefit.service;

import com.utefit.config.VnpayProperties;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VnpayPaymentServiceTest {
    @Mock VnpayProperties properties;
    @Mock UserRepository users;
    @Mock MembershipRepository memberships;
    @Mock PaymentRepository payments;
    @Mock MembershipPaymentService billing;
    private VnpayGateway gateway;
    private VnpayPaymentService service;
    private User member;
    private Membership membership;
    private Payment payment;

    @BeforeEach
    void setup() {
        lenient().when(properties.isEnabled()).thenReturn(true);
        lenient().when(properties.getTmnCode()).thenReturn("TESTTMN");
        lenient().when(properties.getHashSecret()).thenReturn("unit-test-only-key");
        lenient().when(properties.getReturnUrl()).thenReturn("http://localhost/payments/vnpay/return");
        gateway = new VnpayGateway(properties);
        service = new VnpayPaymentService(properties, gateway, users, memberships, payments, billing);
        member = User.builder().id(1L).email("member@utefit.test").role(UserRole.MEMBER)
                .status(UserStatus.ACTIVE).enabled(true).build();
        membership = Membership.builder().id(2L).user(member).status(MembershipStatus.PENDING)
                .branch(Branch.builder().id(3L).active(true).build())
                .plan(MembershipPlan.builder().id(4L).active(true).price(new BigDecimal("150000")).durationDays(30).build())
                .quotedPrice(new BigDecimal("100000")).quotedDurationDays(30).build();
        payment = Payment.builder().id(5L).user(member).membership(membership)
                .method(PaymentMethod.VNPAY).status(PaymentStatus.PENDING)
                .amount(new BigDecimal("100000")).transactionCode("UT7test")
                .createdAt(LocalDateTime.now()).build();
    }

    private Map<String, String> callback(String response, String status) {
        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_TmnCode", "TESTTMN");
        fields.put("vnp_TxnRef", "UT7test");
        fields.put("vnp_Amount", "10000000");
        fields.put("vnp_ResponseCode", response);
        fields.put("vnp_TransactionStatus", status);
        fields.put("vnp_TransactionNo", "12345");
        fields.put("vnp_PayDate", "20261025120000");
        fields.put("vnp_SecureHash", gateway.signature(fields));
        return fields;
    }

    private void found() {
        when(payments.findByTransactionCode("UT7test")).thenReturn(Optional.of(payment));
        when(memberships.lockById(2L)).thenReturn(Optional.of(membership));
        when(payments.lockById(5L)).thenReturn(Optional.of(payment));
    }

    @Test
    void validIpnPaysOnce() {
        found();
        var fields = callback("00", "00");
        assertEquals("00", service.ipn(fields).get("RspCode"));
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals("02", service.ipn(fields).get("RspCode"));
        verify(billing, times(1)).activateMembership(eq(membership), any());
        verify(payments, times(1)).save(payment);
    }

    @Test
    void forgedIpnDoesNotReadOrWriteDatabase() {
        var fields = callback("00", "00");
        fields.put("vnp_Amount", "1");
        assertEquals("97", service.ipn(fields).get("RspCode"));
        verifyNoInteractions(payments, memberships, billing);
    }

    @Test
    void signedWrongAmountDoesNotPay() {
        found();
        var fields = callback("00", "00");
        fields.put("vnp_Amount", "20000000");
        fields.put("vnp_SecureHash", gateway.signature(fields));
        assertEquals("04", service.ipn(fields).get("RspCode"));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(billing);
    }

    @Test
    void unknownOrderDoesNotPay() {
        when(payments.findByTransactionCode("UT7test")).thenReturn(Optional.empty());
        assertEquals("01", service.ipn(callback("00", "00")).get("RspCode"));
        verifyNoInteractions(memberships, billing);
    }

    @Test
    void failedOrCancelledTransactionDoesNotActivate() {
        found();
        assertEquals("00", service.ipn(callback("24", "02")).get("RspCode"));
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verifyNoInteractions(billing);
    }

    @Test
    void inconsistentSuccessCodesDoNotActivate() {
        found();
        service.ipn(callback("00", "02"));
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verifyNoInteractions(billing);
    }

    @Test
    void returnNeverConfirmsPayment() {
        when(payments.findByTransactionCode("UT7test")).thenReturn(Optional.of(payment));
        assertTrue(service.returnMessage(callback("00", "00")).contains("Đang chờ"));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(payments, never()).save(any());
        verifyNoInteractions(billing, memberships);
    }

    @Test
    void cashPaymentCannotBeConfirmedByVnpay() {
        payment.setMethod(PaymentMethod.CASH);
        when(payments.findByTransactionCode("UT7test")).thenReturn(Optional.of(payment));
        assertEquals("01", service.ipn(callback("00", "00")).get("RspCode"));
        verifyNoInteractions(memberships, billing);
    }

    @Test
    void doesNotReactivateCancelledMembershipButPreservesReceivedMoney() {
        found();
        membership.setStatus(MembershipStatus.CANCELLED);
        service.ipn(callback("00", "00"));
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        verifyNoInteractions(billing);
    }

    @Test
    void differentOwnerCannotStartPayment() {
        when(users.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        membership.setUser(User.builder().id(99L).build());
        when(memberships.lockById(2L)).thenReturn(Optional.of(membership));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.start(member.getEmail(), 2L, "127.0.0.1"));
        verifyNoInteractions(payments);
    }

    @Test
    void newPaymentUsesQuotedPriceAndSandbox() {
        when(users.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(memberships.lockById(2L)).thenReturn(Optional.of(membership));
        when(payments.findByMembership_IdAndStatus(2L, PaymentStatus.PENDING)).thenReturn(List.of());
        when(payments.saveAndFlush(any())).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(5L);
            assertEquals(new BigDecimal("100000"), saved.getAmount());
            assertEquals(PaymentStatus.PENDING, saved.getStatus());
            return saved;
        });
        String url = service.start(member.getEmail(), 2L, "127.0.0.1");
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/"));
        assertTrue(url.contains("vnp_Amount=10000000"));
        assertTrue(url.contains("vnp_SecureHash="));
        verifyNoInteractions(billing);
    }

    @Test
    void repeatedStartReusesPendingPayment() {
        when(users.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(memberships.lockById(2L)).thenReturn(Optional.of(membership));
        when(payments.findByMembership_IdAndStatus(2L, PaymentStatus.PENDING)).thenReturn(List.of(payment));
        assertEquals(service.start(member.getEmail(), 2L, "127.0.0.1"),
                service.start(member.getEmail(), 2L, "127.0.0.1"));
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void pendingCashCannotBeBypassed() {
        payment.setMethod(PaymentMethod.CASH);
        when(users.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(memberships.lockById(2L)).thenReturn(Optional.of(membership));
        when(payments.findByMembership_IdAndStatus(2L, PaymentStatus.PENDING)).thenReturn(List.of(payment));
        assertThrows(IllegalStateException.class, () -> service.start(member.getEmail(), 2L, "127.0.0.1"));
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void signatureChecksMerchantAndHex() {
        var fields = callback("00", "00");
        assertTrue(gateway.verified(fields));
        fields.put("vnp_TmnCode", "OTHER");
        fields.put("vnp_SecureHash", gateway.signature(fields));
        assertFalse(gateway.verified(fields));
        fields.put("vnp_SecureHash", "invalid");
        assertFalse(gateway.verified(fields));
    }
}
