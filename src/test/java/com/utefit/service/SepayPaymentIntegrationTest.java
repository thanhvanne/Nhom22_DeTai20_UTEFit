package com.utefit.service;

import com.utefit.config.SepayProperties;
import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SepayPaymentIntegrationTest {
    @Autowired SepayPaymentService service;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean MembershipPaymentService billing;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired PaymentRepository payments;
    @Autowired SepayPaymentDetailsRepository details;
    @Autowired SepayWebhookTransactionRepository receipts;
    @Autowired NotificationRepository notifications;
    @Autowired WebApplicationContext context;
    @MockitoBean SepayProperties properties;
    @MockitoBean SepayDatabaseGuard guard;
    private static final String SECRET = "isolated-hmac-fixture-never-use-for-live-123456";
    private static final AtomicLong IDS = new AtomicLong(10000);
    private MockMvc mvc;
    private User member;
    private Long membershipId;
    private Long paymentId;
    private String reference;

    @BeforeEach
    void setup() {
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                        .createNativeQuery("CREATE UNIQUE INDEX IF NOT EXISTS test_sepay_bank_reference ON sepay_webhook_transactions(environment,gateway,account_number,bank_reference)").executeUpdate());
        when(properties.testMode()).thenReturn(true);
        when(properties.acceptingNewPayments()).thenReturn(true);
        when(properties.mode()).thenReturn("TEST");
        when(properties.secret()).thenReturn(SECRET);
        when(properties.getBankCode()).thenReturn("TestBank");
        when(properties.getGateway()).thenReturn("TESTBANK");
        when(properties.getAccountNumber()).thenReturn("TEST0001");
        when(properties.getAccountName()).thenReturn("Isolated QA");
        when(properties.getPaymentPrefix()).thenReturn("UTE");
        when(properties.getPaymentMinutes()).thenReturn(15);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        member = users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("QA").lastName("SePay")
                .password("unused-fixture").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        Branch branch = branches.save(Branch.builder().name("SePay QA").address("QA").active(true).build());
        MembershipPlan plan = plans.save(MembershipPlan.builder().name("QA snapshot").price(new BigDecimal("100000"))
                .durationDays(30).branchIds(Set.of(branch.getId())).active(true).build());
        var request = new MembershipPurchaseRequest();
        request.setBranchId(branch.getId()); request.setPlanId(plan.getId());
        membershipId = billing.createMembership(member.getEmail(), request);
        paymentId = service.start(member.getEmail(), membershipId);
        reference = details.findById(paymentId).orElseThrow().getPaymentReference();
    }

    private User cancellationAdmin() {
        return users.save(User.builder().email(UUID.randomUUID() + "@cancel.test").firstName("Admin").lastName("QA")
                .password("unused").role(UserRole.ADMIN).status(UserStatus.ACTIVE).enabled(true).build());
    }

    private Long newUnpaidMembership() {
        var original = memberships.findById(membershipId).orElseThrow();
        var plan = plans.save(MembershipPlan.builder().name("Cheaper QA " + UUID.randomUUID()).price(new BigDecimal("20000"))
                .durationDays(1).branchIds(Set.of(original.getBranch().getId())).active(true).build());
        var input = new MembershipPurchaseRequest(); input.setBranchId(original.getBranch().getId()); input.setPlanId(plan.getId());
        return billing.createMembership(member.getEmail(), input);
    }

    @Test void noPaymentAndUnconfirmedCashCanBeCancelledImmediately() {
        Long noPayment = newUnpaidMembership();
        assertTrue(billing.cancelPendingMembership(member.getEmail(), noPayment));
        assertEquals(MembershipStatus.CANCELLED, memberships.findById(noPayment).orElseThrow().getStatus());
        Long cashOrder = newUnpaidMembership();
        Long cashId = billing.requestCashPayment(member.getEmail(), cashOrder);
        assertTrue(billing.cancelPendingMembership(member.getEmail(), cashOrder));
        var cancelled = payments.findById(cashId).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, cancelled.getStatus());
        assertEquals(member.getEmail(), cancelled.getCancelledBy());
        assertThrows(IllegalArgumentException.class, () -> billing.confirmCashPayment(cashId));
    }

    @Test void cancellationIsAuditedAdminOnlyCsrfProtectedAndAllowsCheaperPurchase() throws Exception {
        assertFalse(billing.cancelPendingMembership(member.getEmail(), membershipId));
        assertFalse(billing.cancelPendingMembership(member.getEmail(), membershipId));
        assertEquals(PaymentStatus.CANCEL_REQUESTED, payments.findById(paymentId).orElseThrow().getStatus());
        assertNotNull(payments.findById(paymentId).orElseThrow().getCancellationRequestedAt());
        assertThrows(IllegalStateException.class, () -> service.start(member.getEmail(), membershipId));
        assertThrows(IllegalStateException.class, () -> billing.requestCashPayment(member.getEmail(), membershipId));
        var admin = cancellationAdmin();
        mvc.perform(post("/admin/payments/{id}/approve-cancellation", paymentId).with(user(member.getEmail()).roles("MEMBER")).with(csrf())
                        .param("reason", "Kiểm tra lịch sử và duyệt hủy đơn.").param("reviewed", "true")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/payments/{id}/approve-cancellation", paymentId).with(user(admin.getEmail()).roles("ADMIN"))
                        .param("reason", "Kiểm tra lịch sử và duyệt hủy đơn.").param("reviewed", "true")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/payments").param("status", "CANCEL_REQUESTED").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/payments/{id}", paymentId).with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Duyệt hủy gói chờ")));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> billing.approvePendingCancellation(member.getEmail(), paymentId, "Kiểm tra lịch sử và duyệt hủy đơn.", true));
        admin.setEnabled(false); users.saveAndFlush(admin);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> billing.approvePendingCancellation(admin.getEmail(), paymentId, "Kiểm tra lịch sử và duyệt hủy đơn.", true));
        admin.setEnabled(true); users.saveAndFlush(admin);
        assertThrows(IllegalArgumentException.class,
                () -> billing.approvePendingCancellation(admin.getEmail(), paymentId, "Không đủ", false));
        assertEquals(PaymentStatus.CANCEL_REQUESTED, payments.findById(paymentId).orElseThrow().getStatus());
        mvc.perform(post("/admin/payments/{id}/approve-cancellation", paymentId).with(user(admin.getEmail()).roles("ADMIN")).with(csrf())
                        .param("reason", "Kiểm tra lịch sử và duyệt hủy đơn.").param("reviewed", "true")).andExpect(status().is3xxRedirection());
        var cancelled = payments.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, cancelled.getStatus()); assertEquals(admin.getEmail(), cancelled.getCancelledBy());
        assertNotNull(cancelled.getCancelledAt()); assertNull(cancelled.getPaidAt());
        assertEquals(MembershipStatus.CANCELLED, memberships.findById(membershipId).orElseThrow().getStatus());
        Long replacement = newUnpaidMembership();
        Long cash = billing.requestCashPayment(member.getEmail(), replacement);
        assertEquals(0, new BigDecimal("20000").compareTo(payments.findById(cash).orElseThrow().getAmount()));
    }

    @Test void paidOrdersCannotUseUnpaidCancellation() throws Exception {
        assertEquals(200, deliver(valid(IDS.incrementAndGet())));
        assertThrows(IllegalStateException.class, () -> billing.cancelPendingMembership(member.getEmail(), membershipId));
        assertThrows(IllegalStateException.class, () -> billing.approvePendingCancellation(cancellationAdmin().getEmail(), paymentId, "Không được hủy đơn đã thu tiền.", true));
        assertEquals(PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test void lateAuthenticatedWebhookAfterCancellationIsDurableReviewAndDuplicateIsHarmless() throws Exception {
        billing.cancelPendingMembership(member.getEmail(), membershipId);
        billing.approvePendingCancellation(cancellationAdmin().getEmail(), paymentId, "Đã kiểm tra và đóng đơn theo yêu cầu.", true);
        long receiptId = IDS.incrementAndGet(); String payload = valid(receiptId);
        assertEquals(200, deliver(payload)); assertEquals(200, deliver(payload));
        assertEquals("CANCELLED_ORDER_REQUIRES_REFUND_REVIEW", receipts.findByEnvironmentAndTransactionId("TEST", receiptId).orElseThrow().getReason());
        assertEquals(PaymentStatus.CANCELLED, payments.findById(paymentId).orElseThrow().getStatus());
        assertNull(memberships.findById(membershipId).orElseThrow().getStartDate());
        assertEquals("REVIEW", service.status(member.getEmail(), paymentId).displayState());
    }

    @Test void requestedCancellationAlsoFreezesSettlementBeforeAdminApproval() throws Exception {
        billing.cancelPendingMembership(member.getEmail(), membershipId);
        long receiptId = IDS.incrementAndGet();
        assertEquals(200, deliver(valid(receiptId)));
        assertEquals("CANCELLATION_REQUESTED_REVIEW", receipts.findByEnvironmentAndTransactionId("TEST", receiptId).orElseThrow().getReason());
        assertEquals(PaymentStatus.CANCEL_REQUESTED, payments.findById(paymentId).orElseThrow().getStatus());
        mvc.perform(get("/member/memberships").with(user(member.getEmail()).roles("MEMBER"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã yêu cầu hủy")));
    }

    @Test void cancellationAuditAndStatusesRollbackTogetherOnFailure() {
        billing.cancelPendingMembership(member.getEmail(), membershipId);
        var admin = cancellationAdmin();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            billing.approvePendingCancellation(admin.getEmail(), paymentId, "Đã kiểm tra và duyệt hủy trên H2 riêng.", true);
            context.getBean(jakarta.persistence.EntityManager.class).flush();
            throw new IllegalStateException("H2 forced failure after flush");
        }));
        var original = payments.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.CANCEL_REQUESTED, original.getStatus());
        assertNull(original.getCancelledAt()); assertNull(original.getCancelledBy());
        assertEquals(MembershipStatus.PENDING, memberships.findById(membershipId).orElseThrow().getStatus());
    }

    @Test void memberCannotCancelAnotherMembersOrder() {
        var stranger = users.save(User.builder().email(UUID.randomUUID() + "@stranger.test").firstName("Other").lastName("QA")
                .password("unused").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        assertThrows(IllegalArgumentException.class, () -> billing.cancelPendingMembership(stranger.getEmail(), membershipId));
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test void webhookAndCancellationRaceHasOnlyPaidOrFrozenOutcome() throws Exception {
        var executor = Executors.newFixedThreadPool(2); var gate = new CountDownLatch(1);
        String payload = valid(IDS.incrementAndGet());
        try {
            var cancel = executor.submit(() -> { gate.await(); try { billing.cancelPendingMembership(member.getEmail(), membershipId); return true; }
                catch (IllegalStateException ex) { return false; } });
            var callback = executor.submit(() -> { gate.await(); return deliver(payload); }); gate.countDown();
            assertEquals(200, callback.get(30, TimeUnit.SECONDS));
            boolean requested = cancel.get(30, TimeUnit.SECONDS);
            assertEquals(requested ? PaymentStatus.CANCEL_REQUESTED : PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
            assertEquals(requested ? MembershipStatus.PENDING : MembershipStatus.ACTIVE, memberships.findById(membershipId).orElseThrow().getStatus());
        } finally { executor.shutdownNow(); }
    }

    @Test
    void checkoutTransitionsFromPendingToPaidAndReloadIsReadOnly() throws Exception {
        when(properties.isTestQrPreview()).thenReturn(true);
        mvc.perform(get("/member/payments/{id}/sepay?status=PAID", paymentId).with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sepay-transfer\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sepay-qr-image\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"sepay-success\""))));
        assertEquals(200, deliver(valid(IDS.incrementAndGet())));
        var activated = memberships.findById(membershipId).orElseThrow();
        long paymentCount = payments.count(); long membershipCount = memberships.count();
        mvc.perform(get("/member/payments/{id}/sepay/status", paymentId).with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayState").value("PAID"))
                .andExpect(jsonPath("$.periodState").value("CURRENT"));
        for (int reload = 0; reload < 2; reload++) {
            mvc.perform(get("/member/payments/{id}/sepay", paymentId).with(user(member.getEmail()).roles("MEMBER")))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Thanh toán thành công!")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Gói đang hiệu lực")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sepay-success\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"sepay-transfer\""))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-copy="))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("TEST0001"))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("vietqr.app/img"))));
        }
        assertEquals(paymentCount, payments.count()); assertEquals(membershipCount, memberships.count());
        assertEquals(activated.getEndDate(), memberships.findById(membershipId).orElseThrow().getEndDate());
    }

    @Test
    void paidFutureRenewalShowsItsActualPeriodWithoutClaimingCurrentAccess() throws Exception {
        assertEquals(200, deliver(valid(IDS.incrementAndGet())));
        var prior = memberships.findById(membershipId).orElseThrow();
        membershipId = billing.renew(member.getEmail(), membershipId);
        paymentId = service.start(member.getEmail(), membershipId);
        reference = details.findById(paymentId).orElseThrow().getPaymentReference();
        assertEquals(200, deliver(valid(IDS.incrementAndGet())));
        var summary = service.checkout(member.getEmail(), paymentId).membership();
        assertTrue(summary.renewal()); assertEquals("FUTURE", summary.periodState());
        assertEquals(prior.getEndDate().plusDays(1), summary.startDate());
        mvc.perform(get("/member/payments/{id}/sepay", paymentId).with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Kỳ tập đã thanh toán, chưa bắt đầu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thời hạn kỳ gia hạn")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("30 ngày")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Gói đang hiệu lực"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"sepay-transfer\""))));
    }

    @Test
    void signedCalendarRenewalUsesSnapshotAndDuplicateDoesNotAppendAgain() throws Exception {
        assertEquals(200, deliver(valid(IDS.incrementAndGet())));
        var previous = memberships.findById(membershipId).orElseThrow();
        var plan = plans.findById(previous.getPlan().getId()).orElseThrow();
        plan.setDurationUnit(MembershipDurationUnit.MONTH); plan.setDurationQuantity(3);
        plan.setBonusUnit(MembershipDurationUnit.WEEK); plan.setBonusQuantity(2); plans.saveAndFlush(plan);
        membershipId = billing.renew(member.getEmail(), membershipId);
        paymentId = service.start(member.getEmail(), membershipId);
        reference = details.findById(paymentId).orElseThrow().getPaymentReference();
        plan.setDurationQuantity(1); plans.saveAndFlush(plan);
        String signed = valid(IDS.incrementAndGet());
        assertEquals(200, deliver(signed)); assertEquals(200, deliver(signed));
        var period = memberships.findById(membershipId).orElseThrow();
        assertEquals(previous.getEndDate().plusDays(1), period.getStartDate());
        assertEquals(period.getStartDate().plusMonths(3).plusWeeks(2).minusDays(1), period.getEndDate());
        assertEquals("3 tháng + tặng 2 tuần", service.checkout(member.getEmail(), paymentId).membership().durationDescription());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAILED", "REFUNDED", "EXPIRED", "REVIEW"})
    void checkoutErrorStatesNeverRenderTransferInstructions(String state) throws Exception {
        if (state.equals("REVIEW")) {
            assertEquals(200, deliver(body(IDS.incrementAndGet(), reference, "99999", "TEST0001", "in",
                    LocalDateTime.now(SepayPaymentService.ZONE), UUID.randomUUID().toString())));
        } else if (state.equals("EXPIRED")) {
            new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                    .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                            .createQuery("update SepayPaymentDetails d set d.expiresAt=:date where d.id=:id")
                            .setParameter("date", LocalDateTime.now(SepayPaymentService.ZONE).minusMinutes(1)).setParameter("id", paymentId).executeUpdate());
        } else {
            var payment = payments.findById(paymentId).orElseThrow(); payment.setStatus(PaymentStatus.valueOf(state)); payments.saveAndFlush(payment);
        }
        mvc.perform(get("/member/payments/{id}/sepay", paymentId).with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("data-display-state=\"" + state + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"sepay-transfer\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"sepay-success\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-copy="))));
        mvc.perform(get("/member/payments/{id}/sepay/status", paymentId).with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(jsonPath("$.displayState").value(state));
    }

    private String body(long id, String code, String amount, String account, String type, LocalDateTime date, String bankReference) {
        return "{\"id\":" + id + ",\"gateway\":\"TestBank\",\"accountNumber\":\"" + account
                + "\",\"code\":\"" + code + "\",\"content\":\"" + code + "\",\"transferType\":\"" + type
                + "\",\"transferAmount\":" + amount + ",\"referenceCode\":\"" + bankReference
                + "\",\"transactionDate\":\"" + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\"}";
    }

    private String valid(long id) {
        return body(id, reference, "100000", "TEST0001", "in", LocalDateTime.now(SepayPaymentService.ZONE), "REF" + id);
    }

    private int deliver(String body) throws Exception {
        return deliverTo(body, "/payments/sepay/webhook/test", false);
    }

    private int deliverTo(String body, String path, boolean secure) throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signed = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        return mvc.perform(post(path).secure(secure).contentType("application/json").content(body)
                .header("X-SePay-Timestamp", timestamp).header("X-SePay-Signature", "sha256=" + HexFormat.of().formatHex(signed)))
                .andReturn().getResponse().getStatus();
    }

    private static final String BIDV_MAIN = "8820666613";
    private static final String BIDV_VA = "96247SXBBQ";

    private void bidvOrder(boolean legacy) {
        when(properties.testMode()).thenReturn(false);
        when(properties.mode()).thenReturn("LIVE");
        when(properties.getBankCode()).thenReturn("BIDV");
        when(properties.getGateway()).thenReturn("BIDV");
        when(properties.getAccountNumber()).thenReturn(BIDV_MAIN);
        when(properties.getVaNumber()).thenReturn(BIDV_VA);
        member = users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("VA fixture").lastName("H2 only")
                .password("unused-fixture").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        Branch branch = branches.save(Branch.builder().name("VA isolated").address("H2 only").active(true).build());
        MembershipPlan plan = plans.save(MembershipPlan.builder().name("VA snapshot").price(new BigDecimal("50000"))
                .durationDays(30).active(true).build());
        Membership membership = memberships.save(Membership.builder().user(member).branch(branch).plan(plan)
                .status(MembershipStatus.PENDING).quotedPrice(new BigDecimal("50000")).quotedDurationDays(30).build());
        membershipId = membership.getId();
        if (legacy) {
            reference = "UTE" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase(java.util.Locale.ROOT);
            Payment payment = payments.saveAndFlush(Payment.builder().user(member).membership(membership)
                    .amount(new BigDecimal("50000")).method(PaymentMethod.SEPAY).status(PaymentStatus.PENDING)
                    .transactionCode(reference).createdAt(LocalDateTime.now(SepayPaymentService.ZONE)).build());
            paymentId = payment.getId();
            details.saveAndFlush(SepayPaymentDetails.builder().payment(payment).paymentReference(reference).environment("LIVE")
                    .bankCode("BIDV").gateway("BIDV").accountNumber(BIDV_MAIN).accountName("Isolated fixture")
                    .expiresAt(LocalDateTime.now(SepayPaymentService.ZONE).plusMinutes(15)).build());
        } else {
            paymentId = service.start(member.getEmail(), membershipId);
            reference = details.findById(paymentId).orElseThrow().getPaymentReference();
        }
    }

    private String bidvBody(long id, String subAccount) {
        return body(id, reference, "50000", BIDV_MAIN, "in", LocalDateTime.now(SepayPaymentService.ZONE), "REF" + id)
                .replace("\"gateway\":\"TestBank\"", "\"gateway\":\"BIDV\",\"subAccount\":\"" + subAccount + "\"");
    }

    @Test
    void bidvQrUsesVaSnapshotAndValidSignedReceiptPaysExactlyOnce() throws Exception {
        bidvOrder(false);
        var snapshot = details.findById(paymentId).orElseThrow();
        assertEquals(BIDV_MAIN, snapshot.getAccountNumber()); assertEquals(BIDV_VA, snapshot.getVaNumber());
        var checkout = service.checkout(member.getEmail(), paymentId);
        assertEquals(BIDV_VA, checkout.accountNumber()); assertTrue(checkout.virtualAccount());
        assertTrue(checkout.qrUrl().contains("acc=" + BIDV_VA));
        assertFalse(checkout.qrUrl().contains(BIDV_MAIN));
        assertTrue(checkout.qrUrl().contains("amount=50000")); assertTrue(checkout.qrUrl().contains("des=" + reference));
        mvc.perform(get("/member/payments/{id}/sepay", paymentId).with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(BIDV_VA)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(BIDV_MAIN))));
        long notificationsBefore = notifications.count(); long id = IDS.incrementAndGet(); String payload = bidvBody(id, BIDV_VA);
        assertEquals(200, deliverTo(payload, "/payments/sepay/webhook/live", true));
        assertEquals(200, deliverTo(payload, "/payments/sepay/webhook/live", true));
        assertEquals(PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
        assertEquals(MembershipStatus.ACTIVE, memberships.findById(membershipId).orElseThrow().getStatus());
        var receipt = receipts.findByEnvironmentAndTransactionId("LIVE", id).orElseThrow();
        assertEquals("MATCHED", receipt.getOutcome()); assertEquals(BIDV_MAIN, receipt.getAccountNumber());
        assertEquals(BIDV_VA, receipt.getSubAccount()); assertEquals(notificationsBefore + 1, notifications.count());
    }

    @ParameterizedTest
    @ValueSource(strings={"empty", "missing", "null", "wrongVa", "lowercase", "whitespace", "gateway", "main", "amount", "reference"})
    void bidvMismatchesPersistReviewWithoutActivation(String scenario) throws Exception {
        bidvOrder(false);
        String subAccount = scenario.equals("whitespace") ? BIDV_VA + " " : scenario.equals("wrongVa") ? "ANOTHERVA" : scenario.equals("lowercase") ? BIDV_VA.toLowerCase(java.util.Locale.ROOT)
                : scenario.equals("empty") ? "" : BIDV_VA;
        long id = IDS.incrementAndGet(); String payload = bidvBody(id, subAccount);
        if (scenario.equals("missing")) payload = payload.replace(",\"subAccount\":\"" + BIDV_VA + "\"", "");
        if (scenario.equals("null")) payload = payload.replace("\"subAccount\":\"" + BIDV_VA + "\"", "\"subAccount\":null");
        if (scenario.equals("gateway")) payload = payload.replace("\"gateway\":\"BIDV\"", "\"gateway\":\"WRONGBANK\"");
        if (scenario.equals("main")) payload = payload.replace("\"accountNumber\":\"" + BIDV_MAIN + "\"", "\"accountNumber\":\"WRONGMAIN\"");
        if (scenario.equals("amount")) payload = payload.replace("\"transferAmount\":50000", "\"transferAmount\":49999");
        if (scenario.equals("reference")) payload = payload.replace(reference, "UTE" + "F".repeat(24));
        assertEquals(200, deliverTo(payload, "/payments/sepay/webhook/live", true));
        assertEquals("REVIEW", receipts.findByEnvironmentAndTransactionId("LIVE", id).orElseThrow().getOutcome());
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
        assertEquals(MembershipStatus.PENDING, memberships.findById(membershipId).orElseThrow().getStatus());
    }

    @Test
    void bidvInvalidHmacCannotCreateReceiptOrPay() throws Exception {
        bidvOrder(false); long id = IDS.incrementAndGet();
        mvc.perform(post("/payments/sepay/webhook/live").secure(true).contentType("application/json").content(bidvBody(id, BIDV_VA))
                .header("X-SePay-Timestamp", Instant.now().getEpochSecond()).header("X-SePay-Signature", "sha256=" + "0".repeat(64)))
                .andExpect(status().isUnauthorized());
        assertTrue(receipts.findByEnvironmentAndTransactionId("LIVE", id).isEmpty());
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void bidvLegacyOrderIsNotRetrofittedWithCurrentVaOrAdvertisedForAnotherTransfer() throws Exception {
        bidvOrder(true);
        assertEquals(paymentId, service.start(member.getEmail(), membershipId));
        assertNull(details.findById(paymentId).orElseThrow().getVaNumber());
        var checkout = service.checkout(member.getEmail(), paymentId);
        assertTrue(checkout.legacyTransferBlocked()); assertNull(checkout.qrUrl()); assertNull(checkout.accountNumber());
        mvc.perform(get("/member/payments/{id}/sepay", paymentId).with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Đơn cũ chưa có snapshot")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(BIDV_MAIN))));
        long id = IDS.incrementAndGet(); assertEquals(200, deliverTo(bidvBody(id, BIDV_VA), "/payments/sepay/webhook/live", true));
        assertEquals("VA_SNAPSHOT_CONFIGURATION_MISMATCH", receipts.findByEnvironmentAndTransactionId("LIVE", id).orElseThrow().getReason());
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
        assertNull(details.findById(paymentId).orElseThrow().getVaNumber());
    }

    @Test
    void bidvExpiredOrderStaysReviewEvenWithCorrectVaAndHmac() throws Exception {
        bidvOrder(false);
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                        .createQuery("update SepayPaymentDetails d set d.expiresAt=:date where d.id=:id")
                        .setParameter("date", LocalDateTime.now(SepayPaymentService.ZONE).minusMinutes(1)).setParameter("id", paymentId).executeUpdate());
        long id = IDS.incrementAndGet();
        assertEquals(200, deliverTo(bidvBody(id, BIDV_VA), "/payments/sepay/webhook/live", true));
        assertEquals("LATE_OR_INVALID_TRANSACTION_DATE", receipts.findByEnvironmentAndTransactionId("LIVE", id).orElseThrow().getReason());
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void changingConfiguredVaCannotRewriteExistingSnapshotOrConfirmItForDifferentVa() throws Exception {
        bidvOrder(false);
        when(properties.getVaNumber()).thenReturn("NEWVA123");
        var checkout = service.checkout(member.getEmail(), paymentId);
        assertEquals(BIDV_VA, checkout.accountNumber()); assertNull(checkout.qrUrl());
        assertEquals(BIDV_VA, details.findById(paymentId).orElseThrow().getVaNumber());
        long id = IDS.incrementAndGet();
        assertEquals(200, deliverTo(bidvBody(id, "NEWVA123"), "/payments/sepay/webhook/live", true));
        assertEquals("VA_SNAPSHOT_CONFIGURATION_MISMATCH", receipts.findByEnvironmentAndTransactionId("LIVE", id).orElseThrow().getReason());
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void bidvConcurrentDuplicateHasOneReceiptAndOneActivation() throws Exception {
        bidvOrder(false);
        long id = IDS.incrementAndGet(); String payload = bidvBody(id, BIDV_VA); long notificationsBefore = notifications.count();
        var executor = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            Callable<Integer> task = () -> { start.await(); return deliverTo(payload, "/payments/sepay/webhook/live", true); };
            var first = executor.submit(task); var second = executor.submit(task); start.countDown();
            assertEquals(200, first.get(30, TimeUnit.SECONDS)); assertEquals(200, second.get(30, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
        assertEquals(1, receipts.findAll().stream().filter(receipt -> receipt.getEnvironment().equals("LIVE") && receipt.getTransactionId().equals(id)).count());
        assertEquals(notificationsBefore + 1, notifications.count());
        assertEquals(PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void duplicateWithAlteredSubAccountIsAuditedWithoutReplacingOriginalReceipt() throws Exception {
        bidvOrder(false); long id = IDS.incrementAndGet();
        assertEquals(200, deliverTo(bidvBody(id, BIDV_VA), "/payments/sepay/webhook/live", true));
        assertEquals(200, deliverTo(bidvBody(id, "ANOTHERVA"), "/payments/sepay/webhook/live", true));
        var receipt = receipts.findByEnvironmentAndTransactionId("LIVE", id).orElseThrow();
        assertEquals(BIDV_VA, receipt.getSubAccount()); assertNotNull(receipt.getConflictHash());
        assertEquals(PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void pendingReuseSnapshotAndWebhookActivateExactlyOnceWithoutPolling() throws Exception {
        assertEquals(paymentId, service.start(member.getEmail(), membershipId));
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
        long id = IDS.incrementAndGet();
        String payload = valid(id);
        assertEquals(200, deliver(payload));
        var activated = memberships.findById(membershipId).orElseThrow();
        assertEquals(MembershipStatus.ACTIVE, activated.getStatus());
        assertEquals(activated.getStartDate().plusDays(29), activated.getEndDate());
        assertEquals(200, deliver(payload));
        assertEquals(PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
        assertEquals(activated.getEndDate(), memberships.findById(membershipId).orElseThrow().getEndDate());
        assertTrue(receipts.findByEnvironmentAndTransactionId("TEST", id).isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"under", "over", "code", "account", "out", "late", "active"})
    void abnormalTransfersPersistWithoutActivation(String scenario) throws Exception {
        long id = IDS.incrementAndGet();
        if (scenario.equals("late")) {
            var order = details.findById(paymentId).orElseThrow();
            org.springframework.test.util.ReflectionTestUtils.setField(order, "expiresAt", LocalDateTime.now(SepayPaymentService.ZONE).minusMinutes(1));
            new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                    .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class).createQuery("update SepayPaymentDetails d set d.expiresAt=:date where d.id=:id")
                            .setParameter("date", order.getExpiresAt()).setParameter("id", paymentId).executeUpdate());
        }
        if (scenario.equals("active")) {
            var membership = memberships.findById(membershipId).orElseThrow();
            membership.setStatus(MembershipStatus.ACTIVE); membership.setStartDate(LocalDate.now()); membership.setEndDate(LocalDate.now().plusDays(5));
            memberships.save(membership);
        }
        String body = body(id, scenario.equals("code") ? "UTE" + "F".repeat(24) : reference,
                scenario.equals("under") ? "99999" : scenario.equals("over") ? "100001" : "100000",
                scenario.equals("account") ? "WRONG0001" : "TEST0001", scenario.equals("out") ? "out" : "in",
                LocalDateTime.now(SepayPaymentService.ZONE), "REF" + id);
        assertEquals(200, deliver(body));
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
        assertNotEquals("MATCHED", receipts.findByEnvironmentAndTransactionId("TEST", id).orElseThrow().getOutcome());
        if (!scenario.equals("active")) assertEquals(MembershipStatus.PENDING, memberships.findById(membershipId).orElseThrow().getStatus());
    }

    @Test
    void priorShortPaymentRequiresManualReviewNotAnotherFullTransfer() throws Exception {
        long id = IDS.incrementAndGet();
        assertEquals(200, deliver(body(id, reference, "1", "TEST0001", "in", LocalDateTime.now(SepayPaymentService.ZONE), "REF" + id)));
        assertTrue(service.status(member.getEmail(), paymentId).reconciliationRequired());
        assertNull(service.checkout(member.getEmail(), paymentId).qrUrl());
        long next = IDS.incrementAndGet();
        assertEquals(200, deliver(valid(next)));
        assertEquals("PRIOR_UNRESOLVED_TRANSFER", receipts.findByEnvironmentAndTransactionId("TEST", next).orElseThrow().getReason());
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void forgedAndWrongEnvironmentWebhooksCannotWrite() throws Exception {
        long count = receipts.count();
        mvc.perform(post("/payments/sepay/webhook/test").contentType("application/json").content(valid(IDS.incrementAndGet())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/payments/sepay/webhook/live").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
        assertEquals(count, receipts.count());
    }

    @Test
    void memberOwnershipCsrfAndAdminOnlyReconciliation() throws Exception {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.status("missing@utefit.test", paymentId));
        User other = users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("Other").lastName("Member")
                .password("unused-fixture").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.start(other.getEmail(), membershipId));
        mvc.perform(get("/member/payments/{id}/sepay/status", paymentId).with(user(other.getEmail()).roles("MEMBER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/member/memberships/{id}/sepay", membershipId).with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/payments/sepay-reconciliation").with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void liveWebhookRequiresHttpsAndCannotUseTestSecret() throws Exception {
        when(properties.testMode()).thenReturn(false);
        when(properties.mode()).thenReturn("LIVE");
        when(properties.secret()).thenReturn("different-live-fixture-secret-123456789012345");
        mvc.perform(post("/payments/sepay/webhook/live").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "sha256=" + HexFormat.of().formatHex(mac.doFinal((timestamp + ".{}").getBytes(StandardCharsets.UTF_8)));
        mvc.perform(post("/payments/sepay/webhook/live").secure(true).contentType("application/json").content("{}")
                .header("X-SePay-Timestamp", timestamp).header("X-SePay-Signature", signature))
                .andExpect(status().isUnauthorized());
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void adminCashConfirmationCannotMarkSepayPaid() {
        assertThrows(IllegalArgumentException.class, () -> billing.confirmCashPayment(paymentId));
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void differentTransfersForSameOrderCannotActivateTwice() throws Exception {
        long first = IDS.incrementAndGet(); long second = IDS.incrementAndGet();
        assertEquals(200, deliver(valid(first))); assertEquals(200, deliver(valid(second)));
        assertEquals("MATCHED", receipts.findByEnvironmentAndTransactionId("TEST", first).orElseThrow().getOutcome());
        assertEquals("PAYMENT_OR_MEMBERSHIP_NOT_PENDING", receipts.findByEnvironmentAndTransactionId("TEST", second).orElseThrow().getReason());
    }

    @Test
    void reusedBankReferenceCannotSettleSecondOrder() throws Exception {
        long first = IDS.incrementAndGet(); long second = IDS.incrementAndGet();
        assertEquals(200, deliver(valid(first)));
        String changed = body(second, "UTE" + "F".repeat(24), "100000", "TEST0001", "in", LocalDateTime.now(SepayPaymentService.ZONE), "REF" + first);
        assertEquals(200, deliver(changed));
        var receipt = receipts.findByEnvironmentAndTransactionId("TEST", first).orElseThrow();
        assertEquals(second, receipt.getConflictTransactionId());
        assertTrue(receipts.findByEnvironmentAndTransactionId("TEST", second).isEmpty());
    }

    @Test
    void currencyOtherThanVndRejectedBeforeLedgerWrite() throws Exception {
        long id = IDS.incrementAndGet();
        assertEquals(400, deliver(valid(id).replace("\"transferAmount\":", "\"currency\":\"USD\",\"transferAmount\":")));
        assertTrue(receipts.findByEnvironmentAndTransactionId("TEST", id).isEmpty());
    }

    @Test
    void activationFailureRollsBackReceiptPaymentAndMembershipThenRetryRecovers() throws Exception {
        long id = IDS.incrementAndGet(); String payload = valid(id);
        doThrow(new IllegalStateException("unit activation failure")).when(billing).activateMembership(any(), any());
        assertEquals(503, deliver(payload));
        assertEquals(PaymentStatus.PENDING, payments.findById(paymentId).orElseThrow().getStatus());
        assertEquals(MembershipStatus.PENDING, memberships.findById(membershipId).orElseThrow().getStatus());
        assertTrue(receipts.findByEnvironmentAndTransactionId("TEST", id).isEmpty());
        doCallRealMethod().when(billing).activateMembership(any(), any());
        assertEquals(200, deliver(payload));
        assertEquals(PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void drainSwitchStopsNewStartsButStillAcceptsIssuedOrder() throws Exception {
        when(properties.acceptingNewPayments()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> service.start(member.getEmail(), membershipId));
        assertEquals(200, deliver(valid(IDS.incrementAndGet())));
        assertEquals(PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
    }

    @Test
    void duplicateConcurrentDeliveriesCommitOneReceiptAndActivation() throws Exception {
        long notificationsBefore = notifications.count();
        long id = IDS.incrementAndGet();
        String payload = valid(id);
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Integer> task = () -> { start.await(); return deliver(payload); };
            var first = executor.submit(task); var second = executor.submit(task); start.countDown();
            assertEquals(200, first.get(30, TimeUnit.SECONDS)); assertEquals(200, second.get(30, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
        assertEquals(PaymentStatus.PAID, payments.findById(paymentId).orElseThrow().getStatus());
        assertEquals(MembershipStatus.ACTIVE, memberships.findById(membershipId).orElseThrow().getStatus());
        assertEquals(1, receipts.findAll().stream().filter(receipt -> receipt.getTransactionId().equals(id)).count());
        assertEquals(notificationsBefore + 1, notifications.count());
    }
}
