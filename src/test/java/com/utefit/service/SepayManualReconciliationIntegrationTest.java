package com.utefit.service;

import com.utefit.config.SepayProperties;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SepayManualReconciliationIntegrationTest {
    @Autowired SepayManualReconciliationService service;
    @Autowired SepayWebhookService webhooks;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired PaymentRepository payments;
    @Autowired SepayPaymentDetailsRepository details;
    @Autowired SepayBankReconciliationRepository records;
    @Autowired SepayWebhookTransactionRepository receipts;
    @Autowired NotificationRepository notifications;
    @Autowired WebApplicationContext context;
    @MockitoBean SepayProperties properties;
    @MockitoBean SepayDatabaseGuard guard;
    @MockitoSpyBean MembershipPaymentService billing;
    private User admin;
    private Payment payment;
    private String reference;
    private String bankReference;
    private LocalDateTime bankDate;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        when(properties.mode()).thenReturn("LIVE");
        when(properties.testMode()).thenReturn(false);
        when(properties.getGateway()).thenReturn("BIDV");
        when(properties.getAccountNumber()).thenReturn("8820666613");
        when(properties.getVaNumber()).thenReturn("96247SXBBQ");
        when(properties.secret()).thenReturn("isolated-hmac-fixture-never-use-for-live-123456");
        admin = users.save(User.builder().email(UUID.randomUUID() + "@admin.test").password("unused")
                .firstName("Admin").lastName("H2").role(UserRole.ADMIN).status(UserStatus.ACTIVE).enabled(true).build());
        authenticate(admin.getEmail(), "ROLE_ADMIN");
        ReflectionTestUtils.setField(service, "enabled", true);
        User member = users.save(User.builder().email(UUID.randomUUID() + "@member.test").password("unused")
                .firstName("Member").lastName("H2").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        Branch branch = branches.save(Branch.builder().name("Isolated H2").address("H2").active(true).build());
        MembershipPlan plan = plans.save(MembershipPlan.builder().name("Snapshot").price(new BigDecimal("50000"))
                .durationDays(30).active(true).build());
        Membership membership = memberships.save(Membership.builder().user(member).branch(branch).plan(plan)
                .quotedPrice(new BigDecimal("50000")).quotedDurationDays(30).status(MembershipStatus.PENDING).build());
        bankDate = LocalDateTime.now(SepayPaymentService.ZONE).minusMinutes(2).withNano(0);
        reference = "UTE" + UUID.randomUUID().toString().replace("-", "").substring(0,24).toUpperCase(Locale.ROOT);
        bankReference = "BANK" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        payment = payments.saveAndFlush(Payment.builder().user(member).membership(membership).amount(new BigDecimal("50000"))
                .method(PaymentMethod.SEPAY).status(PaymentStatus.PENDING).transactionCode(reference).createdAt(bankDate.minusMinutes(1)).build());
        details.saveAndFlush(SepayPaymentDetails.builder().payment(payment).environment("LIVE").paymentReference(reference)
                .gateway("BIDV").bankCode("BIDV").accountNumber("8820666613").vaNumber("96247SXBBQ")
                .accountName("Isolated H2 fixture").expiresAt(bankDate.plusMinutes(15)).build());
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                        .createNativeQuery("CREATE UNIQUE NULLS DISTINCT INDEX IF NOT EXISTS test_manual_refund_ref ON sepay_bank_reconciliations(refund_reference)").executeUpdate());
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void authenticate(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, "unused", List.of(new SimpleGrantedAuthority(role))));
    }
    private SepayManualReconciliationService.ReceiptEvidence evidence() {
        return new SepayManualReconciliationService.ReceiptEvidence("BIDV", "8820666613", "96247SXBBQ", reference,
                new BigDecimal("50000"), bankReference, bankDate, "OFFICIAL_BANK_STATEMENT",
                "Internal statement reference H2-ONLY, row independently verified at receiving bank.",
                "Webhook unavailable; receiving bank receipt independently verified.", true);
    }
    private SepayManualReconciliationService.RefundEvidence refund(String bankRef) {
        return new SepayManualReconciliationService.RefundEvidence(bankRef, new BigDecimal("50000"), bankDate.plusMinutes(1),
                "OFFICIAL_BANK_STATEMENT", "Official receiving bank statement confirms outgoing refund completed, internal H2 reference.",
                "Original sender matched to refund beneficiary using internal verification record.", "Member requested refund instead of pending membership activation.", true);
    }
    private void assertPending() {
        assertEquals(PaymentStatus.PENDING, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(MembershipStatus.PENDING, memberships.findById(payment.getMembership().getId()).orElseThrow().getStatus());
    }

    @Test void verifyDoesNotActivateAndUsesExplicitAudit() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        assertPending();
        var record = records.findById(id).orElseThrow();
        assertEquals("MANUAL_BANK_RECONCILIATION", record.getMethod());
        assertEquals(admin.getId(), record.getVerifiedBy().getId());
        assertNotNull(record.getVerifiedAt());
        assertFalse(properties.acceptingNewPayments());
        verify(guard).requireSafeDatabase();
    }
    @Test void verifiedReceiptActivatesExactlyOnceUsingSnapshot() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        service.activate(payment.getId(), id);
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
        var membership = memberships.findById(payment.getMembership().getId()).orElseThrow();
        assertEquals(MembershipStatus.ACTIVE, membership.getStatus());
        assertEquals(bankDate.toLocalDate().plusDays(29), membership.getEndDate());
        assertThrows(IllegalStateException.class, () -> service.activate(payment.getId(), id));
        verify(billing, times(1)).activateMembership(any(), any());
    }
    @Test void missingEvidenceAndSenderScreenshotAreRejected() {
        var original = evidence();
        var missing = new SepayManualReconciliationService.ReceiptEvidence(original.gateway(), original.accountNumber(), original.subAccount(),
                reference, original.amount(), bankReference, bankDate, "SENDER_SCREENSHOT", "image", original.reason(), true);
        assertThrows(IllegalStateException.class, () -> service.verifyReceipt(payment.getId(), missing));
        assertPending();
        assertTrue(records.findByPayment_IdOrderByIdDesc(payment.getId()).isEmpty());
    }
    @Test void memberAuthorityCannotAccessReconciliation() {
        authenticate(admin.getEmail(), "ROLE_MEMBER");
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.view(payment.getId()));
        assertPending();
    }
    @Test void disabledManualFeatureAndLiveGuardCannotBeBypassed() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.verifyReceipt(payment.getId(), evidence()));
        ReflectionTestUtils.setField(service, "enabled", true);
        doThrow(new IllegalStateException("Live approval unavailable")).when(guard).requireSafeDatabase();
        assertThrows(IllegalStateException.class, () -> service.verifyReceipt(payment.getId(), evidence()));
        assertPending();
    }
    @Test void wrongAmountAndVaCannotBeConfirmed() {
        var original = evidence();
        var wrong = new SepayManualReconciliationService.ReceiptEvidence(original.gateway(), original.accountNumber(), "WRONGVA",
                reference, BigDecimal.ONE, bankReference, bankDate, original.source(), original.evidence(), original.reason(), true);
        assertThrows(IllegalStateException.class, () -> service.verifyReceipt(payment.getId(), wrong));
        assertPending();
    }
    @Test void duplicateBankReferenceAcrossOrdersIsRejected() {
        service.verifyReceipt(payment.getId(), evidence());
        var secondMember = users.save(User.builder().email(UUID.randomUUID() + "@other.test").password("unused")
                .role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).firstName("Other").lastName("H2").build());
        var originalMembership = memberships.findById(payment.getMembership().getId()).orElseThrow();
        var secondMembership = memberships.save(Membership.builder().user(secondMember).branch(originalMembership.getBranch())
                .plan(originalMembership.getPlan()).status(MembershipStatus.PENDING).quotedPrice(payment.getAmount()).quotedDurationDays(30).build());
        var other = payments.saveAndFlush(Payment.builder().user(secondMember).membership(secondMembership).method(PaymentMethod.SEPAY)
                .status(PaymentStatus.PENDING).amount(payment.getAmount()).transactionCode(reference + "A").createdAt(bankDate.minusMinutes(1)).build());
        details.saveAndFlush(SepayPaymentDetails.builder().payment(other).paymentReference(reference + "A").environment("LIVE")
                .gateway("BIDV").bankCode("BIDV").accountNumber("8820666613").vaNumber("96247SXBBQ")
                .accountName("H2").expiresAt(bankDate.plusMinutes(15)).build());
        var original = evidence();
        var duplicate = new SepayManualReconciliationService.ReceiptEvidence(original.gateway(), original.accountNumber(), original.subAccount(),
                reference + "A", original.amount(), bankReference, bankDate, original.source(), original.evidence(), original.reason(), true);
        assertThrows(IllegalStateException.class, () -> service.verifyReceipt(other.getId(), duplicate));
        assertThrows(IllegalStateException.class, () -> service.confirmReceived(other.getId(), bankReference, true));
    }
    private SepayWebhookPayload latePayload(String bankRef) {
        String body = "{\"id\":" + (100000000L + payment.getId()) + ",\"gateway\":\"BIDV\",\"accountNumber\":\"8820666613\",\"subAccount\":\"96247SXBBQ\","
                + "\"code\":\"" + reference + "\",\"content\":\"" + reference + "\",\"transferType\":\"in\",\"transferAmount\":50000,\"referenceCode\":\""
                + bankRef + "\",\"transactionDate\":\"" + bankDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\"}";
        return SepayWebhookPayload.parse(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test void lateWebhookAfterManualActivationIsDurableReviewAndDoesNotActivateTwice() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        service.activate(payment.getId(), id);
        webhooks.accept(latePayload(bankReference));
        webhooks.accept(latePayload(bankReference));
        assertEquals("REVIEW", receipts.findByEnvironmentAndTransactionId("LIVE", 100000000L + payment.getId()).orElseThrow().getOutcome());
        verify(billing, times(1)).activateMembership(any(), any());
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
    }
    @Test void webhookWithNoBankReferenceCannotOverrideManualVerification() {
        service.verifyReceipt(payment.getId(), evidence());
        webhooks.accept(latePayload(""));
        assertPending();
        assertEquals("REVIEW", receipts.findByEnvironmentAndTransactionId("LIVE", 100000000L + payment.getId()).orElseThrow().getOutcome());
    }
    @Test void actualRefundIsSeparateAndCancelsOnlyPendingOrder() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        service.confirmRefund(payment.getId(), id, refund("REFUND" + bankReference));
        assertEquals(PaymentStatus.REFUNDED, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(MembershipStatus.CANCELLED, memberships.findById(payment.getMembership().getId()).orElseThrow().getStatus());
        assertNull(payments.findById(payment.getId()).orElseThrow().getPaidAt());
        assertThrows(IllegalStateException.class, () -> service.activate(payment.getId(), id));
        webhooks.accept(latePayload(bankReference));
        assertEquals(PaymentStatus.REFUNDED, payments.findById(payment.getId()).orElseThrow().getStatus());
    }
    @Test void refundRequiresFullAmountAndVerifiedRecipientEvidence() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        var invalid = new SepayManualReconciliationService.RefundEvidence("REFUND" + bankReference, BigDecimal.ONE,
                bankDate.plusSeconds(30), "OFFICIAL_BANK_STATEMENT", "Insufficient", "", "", false);
        assertThrows(IllegalStateException.class, () -> service.confirmRefund(payment.getId(), id, invalid));
        assertPending();
    }
    @Test void activationFailureRollsBackPaymentAndAudit() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        long notificationCount = notifications.count();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("isolated simulated failure after activation and notification");
        }).when(billing).activateMembership(any(), any());
        assertThrows(IllegalStateException.class, () -> service.activate(payment.getId(), id));
        assertPending();
        assertEquals("VERIFIED", records.findById(id).orElseThrow().getOutcome());
        assertNull(records.findById(id).orElseThrow().getResolvedAt());
        assertEquals(notificationCount, notifications.count());
    }
    @Test void concurrentActivationHasExactlyOneWinner() throws Exception {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        Callable<Boolean> action = () -> {
            authenticate(admin.getEmail(), "ROLE_ADMIN");
            try { start.await(); service.activate(payment.getId(), id); return true; }
            catch (IllegalStateException ex) { return false; }
            finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var first = pool.submit(action); var second = pool.submit(action); start.countDown();
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            verify(billing, times(1)).activateMembership(any(), any());
        } finally { pool.shutdownNow(); }
    }
    @Test void memberGuestAndMissingCsrfCannotPost() throws Exception {
        String path = "/admin/payments/sepay-reconciliation/" + payment.getId() + "/activate";
        for (String role : List.of("MEMBER", "PT")) {
            mvc.perform(post(path).with(user(admin.getEmail()).roles(role)).with(csrf()).param("recordId", "0")).andExpect(status().isForbidden());
        }
        mvc.perform(post(path).with(user(admin.getEmail()).roles("ADMIN")).param("recordId", "0")).andExpect(status().isForbidden());
        mvc.perform(post(path).with(anonymous()).with(csrf()).param("recordId", "0")).andExpect(status().is3xxRedirection());
        assertPending();
    }
    @Test void adminCanRenderExistingReconciliationAndCasePages() throws Exception {
        mvc.perform(get("/admin/payments/sepay-reconciliation").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/payments/sepay-reconciliation/" + payment.getId()).with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        authenticate(admin.getEmail(), "ROLE_ADMIN");
        Long id = service.verifyReceipt(payment.getId(), evidence());
        mvc.perform(get("/admin/payments/sepay-reconciliation/" + payment.getId()).with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        assertEquals("VERIFIED", records.findById(id).orElseThrow().getOutcome());
    }
    @Test void manualFlowDoesNotWeakenWebhookHmac() throws Exception {
        mvc.perform(post("/payments/sepay/webhook/live").secure(true).contentType("application/json")
                .header("X-SePay-Timestamp", Long.toString(Instant.now().getEpochSecond()))
                .header("X-SePay-Signature", "sha256=" + "0".repeat(64)).content("{}"))
                .andExpect(status().isUnauthorized());
        assertPending();
    }

    @Test void validAdminFormBindsEvidenceAndActivatesWithCsrf() throws Exception {
        var input = evidence();
        String path = "/admin/payments/sepay-reconciliation/" + payment.getId();
        mvc.perform(post(path + "/verify").with(user(admin.getEmail()).roles("ADMIN")).with(csrf())
                .param("gateway", input.gateway()).param("accountNumber", input.accountNumber()).param("subAccount", input.subAccount())
                .param("paymentReference", reference).param("amount", "50000").param("bankReference", bankReference)
                .param("bankDate", bankDate.toString()).param("source", input.source()).param("evidence", input.evidence())
                .param("reason", input.reason()).param("independentlyVerified", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("manualSuccess"));
        assertPending();
        Long id = records.findByPayment_IdOrderByIdDesc(payment.getId()).get(0).getId();
        mvc.perform(post(path + "/activate").with(user(admin.getEmail()).roles("ADMIN")).with(csrf()).param("recordId", id.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("manualSuccess"));
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test void databaseRevokedAdminCannotUseStaleRoleAuthority() {
        admin.setRole(UserRole.PT);
        users.saveAndFlush(admin);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.verifyReceipt(payment.getId(), evidence()));
        assertPending();
    }

    @Test void manualVerificationIsForbiddenInTestEnvironment() {
        when(properties.mode()).thenReturn("TEST");
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.verifyReceipt(payment.getId(), evidence()));
        assertPending();
    }

    @Test void lateMoneyCanBeRecordedButNotActivated() {
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                        .createNativeQuery("UPDATE sepay_payment_details SET expires_at = :expiry WHERE payment_id = :payment")
                        .setParameter("expiry", bankDate.minusSeconds(1)).setParameter("payment", payment.getId()).executeUpdate());
        Long id = service.verifyReceipt(payment.getId(), evidence());
        assertThrows(IllegalStateException.class, () -> service.activate(payment.getId(), id));
        assertPending();
        service.confirmRefund(payment.getId(), id, refund("REFUND" + bankReference));
        assertEquals(PaymentStatus.REFUNDED, payments.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test void cancelledOrderCanRecordBankEvidenceAndRefundButNeverActivate() {
        billing.cancelPendingMembership(payment.getUser().getEmail(), payment.getMembership().getId());
        billing.approvePendingCancellation(admin.getEmail(), payment.getId(), "Kiểm tra lịch sử và đóng đơn theo yêu cầu hội viên.", true);
        Long id = service.verifyReceipt(payment.getId(), evidence());
        assertThrows(IllegalStateException.class, () -> service.activate(payment.getId(), id));
        assertEquals(PaymentStatus.CANCELLED, payments.findById(payment.getId()).orElseThrow().getStatus());
        service.confirmRefund(payment.getId(), id, refund("REFUND" + bankReference));
        assertEquals(PaymentStatus.REFUNDED, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertNull(memberships.findById(payment.getMembership().getId()).orElseThrow().getStartDate());
    }

    @Test void legacyMainAccountSnapshotRemainsReconciliableWithoutChangingVa() {
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                        .createNativeQuery("UPDATE sepay_payment_details SET va_number = NULL WHERE payment_id = :payment")
                        .setParameter("payment", payment.getId()).executeUpdate());
        var original = evidence();
        var legacy = new SepayManualReconciliationService.ReceiptEvidence(original.gateway(), original.accountNumber(), "", reference,
                original.amount(), bankReference, bankDate, original.source(), original.evidence(), original.reason(), true);
        Long id = service.verifyReceipt(payment.getId(), legacy);
        service.activate(payment.getId(), id);
        assertNull(details.findById(payment.getId()).orElseThrow().getVaNumber());
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test void oneRefundTransactionCannotCancelTwoOrders() {
        String refundRef = "REFUND" + bankReference;
        Long first = service.verifyReceipt(payment.getId(), evidence());
        service.confirmRefund(payment.getId(), first, refund(refundRef));
        setup();
        Long second = service.verifyReceipt(payment.getId(), evidence());
        assertThrows(IllegalStateException.class, () -> service.confirmRefund(payment.getId(), second, refund(refundRef)));
        assertPending();
        assertEquals("VERIFIED", records.findById(second).orElseThrow().getOutcome());
    }

    @Test void concurrentManualVerificationAndWebhookCannotBothOwnReceipt() throws Exception {
        var payload = latePayload(bankReference);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var manual = pool.submit(() -> {
                authenticate(admin.getEmail(), "ROLE_ADMIN");
                try { start.await(); service.verifyReceipt(payment.getId(), evidence()); return true; }
                catch (IllegalStateException ex) { return false; }
                finally { SecurityContextHolder.clearContext(); }
            });
            var automatic = pool.submit(() -> { start.await(); webhooks.accept(payload); return true; });
            start.countDown();
            boolean manuallyVerified = manual.get(20, TimeUnit.SECONDS);
            assertTrue(automatic.get(20, TimeUnit.SECONDS));
            assertEquals(1, records.findByPayment_IdOrderByIdDesc(payment.getId()).size());
            assertEquals(manuallyVerified ? PaymentStatus.PENDING : PaymentStatus.PAID,
                    payments.findById(payment.getId()).orElseThrow().getStatus());
        } finally { pool.shutdownNow(); }
    }

    private User anotherAdmin() {
        return users.save(User.builder().email(UUID.randomUUID() + "@admin.test").password("unused")
                .firstName("Another Admin").lastName("H2").role(UserRole.ADMIN).status(UserStatus.ACTIVE).enabled(true).build());
    }

    @Test void differentActiveAdminsCanVerifyAndActivateWithoutAllowlist() throws Exception {
        User second = anotherAdmin();
        mvc.perform(get("/admin/payments/sepay-reconciliation/" + payment.getId())
                        .with(user(second.getEmail()).roles("ADMIN"))).andExpect(status().isOk());
        authenticate(second.getEmail(), "ROLE_ADMIN");
        Long id = service.verifyReceipt(payment.getId(), evidence());
        assertPending();
        authenticate(admin.getEmail(), "ROLE_ADMIN");
        service.activate(payment.getId(), id);
        var record = records.findById(id).orElseThrow();
        assertEquals(second.getId(), record.getVerifiedBy().getId());
        assertEquals(admin.getId(), record.getResolvedBy().getId());
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test void differentActiveAdminCanConfirmActualRefundWithoutAllowlist() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        User second = anotherAdmin();
        authenticate(second.getEmail(), "ROLE_ADMIN");
        service.confirmRefund(payment.getId(), id, refund("REFUND" + bankReference));
        var record = records.findById(id).orElseThrow();
        assertEquals(admin.getId(), record.getVerifiedBy().getId());
        assertEquals(second.getId(), record.getResolvedBy().getId());
        assertEquals(PaymentStatus.REFUNDED, payments.findById(payment.getId()).orElseThrow().getStatus());
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> deniedDatabaseAccounts() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(UserRole.MEMBER, UserStatus.ACTIVE, true),
                org.junit.jupiter.params.provider.Arguments.of(UserRole.PT, UserStatus.ACTIVE, true),
                org.junit.jupiter.params.provider.Arguments.of(UserRole.ADMIN, UserStatus.LOCKED, true),
                org.junit.jupiter.params.provider.Arguments.of(UserRole.ADMIN, UserStatus.UNVERIFIED, true),
                org.junit.jupiter.params.provider.Arguments.of(UserRole.ADMIN, UserStatus.ACTIVE, false));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("deniedDatabaseAccounts")
    void currentDatabasePermissionsBlockAllWritesDespiteStaleAdminSession(UserRole role, UserStatus status, boolean enabled) throws Exception {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        admin.setRole(role);
        admin.setStatus(status);
        admin.setEnabled(enabled);
        users.saveAndFlush(admin);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.verifyReceipt(payment.getId(), evidence()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.activate(payment.getId(), id));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.confirmRefund(payment.getId(), id, refund("REFUND" + bankReference)));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.confirmReceived(payment.getId(), bankReference, true));
        assertPending();
        assertEquals("VERIFIED", records.findById(id).orElseThrow().getOutcome());
        assertNull(records.findById(id).orElseThrow().getResolvedBy());
        mvc.perform(post("/admin/payments/sepay-reconciliation/" + payment.getId() + "/activate")
                        .with(user(admin.getEmail()).roles("ADMIN")).with(csrf()).param("recordId", id.toString()))
                .andExpect(status().isForbidden());
    }

    @Test void disablingFeatureBlocksAllWritesForEveryAdmin() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        User second = anotherAdmin();
        ReflectionTestUtils.setField(service, "enabled", false);
        for (User actor : List.of(admin, second)) {
            authenticate(actor.getEmail(), "ROLE_ADMIN");
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.verifyReceipt(payment.getId(), evidence()));
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.activate(payment.getId(), id));
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.confirmRefund(payment.getId(), id, refund("REFUND" + bankReference)));
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.confirmReceived(payment.getId(), bankReference, true));
        }
        assertPending();
    }

    @Test void mutationRefreshesCachedAdminFromDatabase() {
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> {
                    status.setRollbackOnly();
                    var manager = context.getBean(jakarta.persistence.EntityManager.class);
                    assertTrue(manager.find(User.class, admin.getId()).getEnabled());
                    manager.createNativeQuery("UPDATE users SET enabled = false WHERE id = :admin")
                            .setParameter("admin", admin.getId()).executeUpdate();
                    assertThrows(org.springframework.security.access.AccessDeniedException.class,
                            () -> service.verifyReceipt(payment.getId(), evidence()));
                });
        assertPending();
        assertTrue(records.findByPayment_IdOrderByIdDesc(payment.getId()).isEmpty());
    }

    @Test void shortConfirmationUsesBackendSnapshotAndRecordsTimeBasis() {
        service.confirmReceived(payment.getId(), bankReference, true);
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(MembershipStatus.ACTIVE, memberships.findById(payment.getMembership().getId()).orElseThrow().getStatus());
        var record = records.findByPayment_IdOrderByIdDesc(payment.getId()).get(0);
        assertEquals("APPLIED", record.getOutcome());
        assertEquals("8820666613", record.getAccountNumber());
        assertEquals("96247SXBBQ", record.getSubAccount());
        assertEquals(0, new BigDecimal("50000").compareTo(record.getAmount()));
        assertTrue(record.getEvidence().contains("TIME_BASIS=ADMIN_CONFIRMATION"));
        assertEquals(admin.getId(), record.getVerifiedBy().getId());
        assertEquals(admin.getId(), record.getResolvedBy().getId());
        assertFalse(properties.acceptingNewPayments());
        assertThrows(IllegalStateException.class, () -> service.confirmReceived(payment.getId(), bankReference, true));
        verify(billing, times(1)).activateMembership(any(), any());
    }

    @Test void shortConfirmationCannotInventConsentOrBankReference() {
        assertThrows(IllegalStateException.class, () -> service.confirmReceived(payment.getId(), bankReference, false));
        assertThrows(IllegalStateException.class, () -> service.confirmReceived(payment.getId(), "", true));
        assertPending();
        assertTrue(records.findByPayment_IdOrderByIdDesc(payment.getId()).isEmpty());
    }

    @Test void shortConfirmationIsAtomicIfActivationFailsAfterNotification() {
        long notificationCount = notifications.count();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("H2 failure after activation");
        }).when(billing).activateMembership(any(), any());
        assertThrows(IllegalStateException.class, () -> service.confirmReceived(payment.getId(), bankReference, true));
        assertPending();
        assertTrue(records.findByPayment_IdOrderByIdDesc(payment.getId()).isEmpty());
        assertEquals(notificationCount, notifications.count());
    }

    @Test void shortConfirmationPreservesLegacyMainAccountWithoutVa() {
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                        .createNativeQuery("UPDATE sepay_payment_details SET va_number = NULL WHERE payment_id = :payment")
                        .setParameter("payment", payment.getId()).executeUpdate());
        service.confirmReceived(payment.getId(), bankReference, true);
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals("", records.findByPayment_IdOrderByIdDesc(payment.getId()).get(0).getSubAccount());
        assertNull(details.findById(payment.getId()).orElseThrow().getVaNumber());
    }

    @Test void expiredOrderWithoutBankTimeEvidenceCannotUseShortConfirmation() {
        expireOrderBeforeReceipt();
        var error = assertThrows(IllegalStateException.class, () -> service.confirmReceived(payment.getId(), bankReference, true));
        assertTrue(error.getMessage().contains("chưa có bằng chứng thời điểm nhận tiền"));
        assertPending();
        assertTrue(records.findByPayment_IdOrderByIdDesc(payment.getId()).isEmpty());
    }

    private void expireOrderBeforeReceipt() {
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                        .createNativeQuery("UPDATE sepay_payment_details SET expires_at = :expiry WHERE payment_id = :payment")
                        .setParameter("expiry", bankDate.minusSeconds(1)).setParameter("payment", payment.getId()).executeUpdate());
    }

    @Test void existingVerifiedReceiptIsReusedWithoutChangingBankDate() {
        Long id = service.verifyReceipt(payment.getId(), evidence());
        service.confirmReceived(payment.getId(), bankReference, true);
        var record = records.findById(id).orElseThrow();
        assertEquals(bankDate, record.getBankDate());
        assertEquals("APPLIED", record.getOutcome());
        assertEquals(1, records.findByPayment_IdOrderByIdDesc(payment.getId()).size());
    }

    @Test void shortConfirmationDoesNotOverrideReviewWithWrongAmount() {
        var payload = latePayload(bankReference);
        receipts.saveAndFlush(SepayWebhookTransaction.builder().environment("LIVE").transactionId(payload.id())
                .payment(payment).gateway(payload.gateway()).accountNumber(payload.accountNumber()).subAccount(payload.subAccount())
                .paymentReference(reference).bankReference(bankReference).content(reference).transferType("in")
                .amount(new BigDecimal("49999")).transactionDate(bankDate).receivedAt(bankDate.plusSeconds(1))
                .bodyHash(payload.bodyHash()).fingerprint(payload.fingerprint()).outcome("REVIEW").reason("AMOUNT_MISMATCH").build());
        assertThrows(IllegalStateException.class, () -> service.confirmReceived(payment.getId(), bankReference, true));
        assertPending();
        assertTrue(records.findByPayment_IdOrderByIdDesc(payment.getId()).isEmpty());
    }

    @Test void shortConfirmationIsFollowedByReadOnlyPendingChoiceAndSeparateRefundPage() throws Exception {
        String path = "/admin/payments/sepay-reconciliation/" + payment.getId();
        mvc.perform(get(path).param("notReceived", "true").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa nhận tiền")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"amount\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"evidence\""))));
        assertPending();
        assertTrue(records.findByPayment_IdOrderByIdDesc(payment.getId()).isEmpty());
        mvc.perform(get(path + "/refund-details").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"evidence\"")));
        mvc.perform(post(path + "/confirm-received").with(user(admin.getEmail()).roles("ADMIN")).with(csrf())
                        .param("bankReference", bankReference).param("independentlyVerified", "true")
                        .param("amount", "1").param("accountNumber", "ATTACKER").param("subAccount", "ATTACKER")
                        .param("paymentReference", "ATTACKER"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("manualSuccess"));
        assertEquals(0, new BigDecimal("50000").compareTo(payments.findById(payment.getId()).orElseThrow().getAmount()));
        assertEquals("96247SXBBQ", records.findByPayment_IdOrderByIdDesc(payment.getId()).get(0).getSubAccount());
    }

    @Test void shortConfirmationEndpointPreservesCsrfAndRoleChecks() throws Exception {
        String path = "/admin/payments/sepay-reconciliation/" + payment.getId() + "/confirm-received";
        for (String role : List.of("MEMBER", "PT")) {
            mvc.perform(post(path).with(user(admin.getEmail()).roles(role)).with(csrf())
                    .param("bankReference", bankReference).param("independentlyVerified", "true")).andExpect(status().isForbidden());
        }
        mvc.perform(post(path).with(user(admin.getEmail()).roles("ADMIN"))
                .param("bankReference", bankReference).param("independentlyVerified", "true")).andExpect(status().isForbidden());
        assertPending();
    }

    @Test void expiredOrderCanUseAuthenticatedReceiptWithProvenTimelyBankDate() {
        new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class))
                .executeWithoutResult(status -> context.getBean(jakarta.persistence.EntityManager.class)
                        .createNativeQuery("UPDATE sepay_payment_details SET expires_at = :expiry WHERE payment_id = :payment")
                        .setParameter("expiry", bankDate.plusSeconds(30)).setParameter("payment", payment.getId()).executeUpdate());
        webhooks.accept(latePayload(bankReference));
        assertPending();
        service.confirmReceived(payment.getId(), bankReference, true);
        var record = records.findByPayment_IdOrderByIdDesc(payment.getId()).get(0);
        assertEquals(bankDate, record.getBankDate());
        assertTrue(record.getEvidence().contains("TIME_BASIS=AUTHENTICATED_WEBHOOK"));
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test void concurrentShortConfirmationsCreateOneAuditAndActivation() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        Callable<Boolean> action = () -> {
            authenticate(admin.getEmail(), "ROLE_ADMIN");
            try { start.await(); service.confirmReceived(payment.getId(), bankReference, true); return true; }
            catch (IllegalStateException ex) { return false; }
            finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var first = pool.submit(action); var second = pool.submit(action); start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, records.findByPayment_IdOrderByIdDesc(payment.getId()).size());
            verify(billing, times(1)).activateMembership(any(), any());
        } finally { pool.shutdownNow(); }
    }
}
