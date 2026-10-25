package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@Import(QrCheckinIntegrationTest.MissingStatusPage.class)
class QrCheckinIntegrationTest {
    @org.springframework.stereotype.Controller
    static class MissingStatusPage {
        @org.springframework.web.bind.annotation.GetMapping("/member/test-qr-missing-status")
        String page() {
            return "member/qr-checkin";
        }
    }
    @Autowired QrCheckinService qr;
    @Autowired MemberQrStatusService states;
    @Autowired QrCheckinTokenRepository qrTokens;
    @Autowired ManagerAccessService managerAccess;
    @Autowired ManagerOperationsService managerOperations;
    @Autowired AdminCheckinService checkins;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired CheckinRepository history;
    @Autowired PlatformTransactionManager transactions;
    @Autowired WebApplicationContext context;
    @MockitoBean Clock clock;
    private final AtomicReference<Instant> now = new AtomicReference<>();
    private User member;
    private Branch branch;
    private Membership membership;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        now.set(Instant.now());
        when(clock.instant()).thenAnswer(invocation -> now.get());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            member = users.save(User.builder().email(suffix + "@utefit.test").firstName("QA").lastName("Member")
                    .password("unused-test-hash").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).tokenVersion(0).build());
            branch = branches.save(Branch.builder().name("QR " + suffix).address("QA").active(true).build());
            MembershipPlan plan = plans.save(MembershipPlan.builder().name("QR " + suffix).durationDays(30)
                    .price(new BigDecimal("100000")).active(true).build());
            membership = memberships.save(Membership.builder().user(member).branch(branch).plan(plan)
                    .status(MembershipStatus.ACTIVE).startDate(LocalDate.now().minusDays(1))
                    .endDate(LocalDate.now().plusDays(29)).build());
        });
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void validQrUsesManualCheckinAndCheckoutAndCannotReplay() {
        var token = qr.issue(member.getEmail());
        assertFalse(token.value().contains(member.getEmail()));
        qr.consume(token.value(), branch.getId());
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
        var record = checkins.history(member.getEmail(), branch.getId(), null, true, 0).getContent().get(0);
        checkins.checkOut(record.getId());
        assertFalse(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(token.value(), branch.getId()));
    }

    @Test
    void expiredQrDoesNotCheckIn() {
        var token = qr.issue(member.getEmail());
        now.set(now.get().plusSeconds(121));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(token.value(), branch.getId()));
        assertFalse(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
    }

    @Test
    void tamperedQrDoesNotCheckIn() {
        var token = qr.issue(member.getEmail());
        String tampered = token.value().substring(0, 15) + "!" + token.value().substring(16);
        assertThrows(IllegalArgumentException.class, () -> qr.consume(tampered, branch.getId()));
    }

    @Test
    void issuingAnotherQrRevokesOldCode() {
        var old = qr.issue(member.getEmail());
        var current = qr.issue(member.getEmail());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(old.value(), branch.getId()));
        qr.consume(current.value(), branch.getId());
    }

    @Test
    void wrongBranchDoesNotBurnValidToken() {
        Branch other = branches.save(Branch.builder().name("Other " + UUID.randomUUID()).address("QA").active(true).build());
        var token = qr.issue(member.getEmail());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(token.value(), other.getId()));
        qr.consume(token.value(), branch.getId());
    }

    @Test
    void expiredMembershipIsRejected() {
        var token = qr.issue(member.getEmail());
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                memberships.findById(membership.getId()).orElseThrow().setEndDate(LocalDate.now().minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(token.value(), branch.getId()));
    }

    @Test
    void lockedMemberIsRejected() {
        var token = qr.issue(member.getEmail());
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                users.findById(member.getId()).orElseThrow().setStatus(UserStatus.LOCKED));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(token.value(), branch.getId()));
    }

    @Test
    void logoutVersionRevokesQr() {
        var token = qr.issue(member.getEmail());
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                users.findById(member.getId()).orElseThrow().setTokenVersion(1));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(token.value(), branch.getId()));
    }

    @Test
    void inactiveBranchIsRejected() {
        var token = qr.issue(member.getEmail());
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                branches.findById(branch.getId()).orElseThrow().setActive(false));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(token.value(), branch.getId()));
    }

    @Test
    void manualOpenCheckinBlocksQr() {
        var token = qr.issue(member.getEmail());
        checkins.checkIn(member.getEmail(), branch.getId());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(token.value(), branch.getId()));
    }

    @Test
    void concurrentScansCreateOneCheckin() throws Exception {
        var token = qr.issue(member.getEmail());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> scan = () -> {
            start.await();
            try {
                qr.consume(token.value(), branch.getId());
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        };
        try {
            Future<Boolean> first = executor.submit(scan);
            Future<Boolean> second = executor.submit(scan);
            start.countDown();
            int successes = (first.get(15, TimeUnit.SECONDS) ? 1 : 0) + (second.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, checkins.history(member.getEmail(), branch.getId(), null, null, 0).getTotalElements());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void memberAndGuestCannotScanAdminQr() throws Exception {
        mvc.perform(post("/admin/checkins/qr").with(user(member.getEmail()).roles("MEMBER")).with(csrf())
                .param("qrToken", "invalid").param("branchId", branch.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/checkins/qr").with(csrf()).param("qrToken", "invalid")
                .param("branchId", branch.getId().toString())).andExpect(status().is3xxRedirection());
    }

    @Test
    void statusTracksConsumeAndCheckoutWithoutReactivatingToken() throws Exception {
        var issued = qr.issue(member.getEmail());
        mvc.perform(get("/member/qr-checkin/status").param("tokenId", issued.id().toString())
                        .with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.qrState").value("VALID")).andExpect(jsonPath("$.attendanceState").value("NONE"));
        qr.consume(issued.value(), branch.getId());
        var open = states.status(member.getEmail(), issued.id());
        assertEquals("OPEN", open.attendanceState());
        assertEquals("UNAVAILABLE", open.qrState());
        assertFalse(open.canIssue());
        assertThrows(IllegalArgumentException.class, () -> qr.issue(member.getEmail()));
        checkins.checkOut(open.checkinId());
        mvc.perform(get("/member/qr-checkin/status").param("tokenId", issued.id().toString())
                        .with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.attendanceState").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.qrState").value("UNAVAILABLE")).andExpect(jsonPath("$.canIssue").value(true));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(issued.value(), branch.getId()));
        assertNotEquals(issued.id(), qr.issue(member.getEmail()).id());
    }

    @Test
    void statusRequiresMemberAndDoesNotAcceptAnotherMembersToken() throws Exception {
        var issued = qr.issue(member.getEmail());
        User other = users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("Other").lastName("Member")
                .password("unused-test-hash").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).tokenVersion(0).build());
        mvc.perform(get("/member/qr-checkin/status").param("tokenId", issued.id().toString())
                        .with(user(other.getEmail()).roles("MEMBER"))).andExpect(status().isNotFound());
        for (String role : List.of("ADMIN", "PT", "MANAGER")) {
            mvc.perform(get("/member/qr-checkin/status").with(user(member.getEmail()).roles(role)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/member/qr-checkin/status")).andExpect(status().is3xxRedirection());
    }

    @Test
    void manualOpenSessionBlocksIssuanceAndExpiredTokenIsReported() {
        var issued = qr.issue(member.getEmail());
        now.set(now.get().plusSeconds(120));
        assertEquals("EXPIRED", states.status(member.getEmail(), issued.id()).qrState());
        checkins.checkIn(member.getEmail(), branch.getId());
        assertFalse(states.status(member.getEmail(), issued.id()).canIssue());
        assertThrows(IllegalArgumentException.class, () -> qr.issue(member.getEmail()));
    }

    @Test
    void memberQrTemplateRendersBeforeAndAfterIssuanceAndWhileOpen() throws Exception {
        mvc.perform(get("/member/qr-checkin").with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(view().name("member/qr-checkin"));
        mvc.perform(post("/member/qr-checkin").with(user(member.getEmail()).roles("MEMBER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("qrTokenId", "qrImage", "checkinStatus"));
        checkins.checkIn(member.getEmail(), branch.getId());
        mvc.perform(post("/member/qr-checkin").with(user(member.getEmail()).roles("MEMBER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
                .andExpect(model().attributeDoesNotExist("qrImage"));
    }

    @Test
    void completedCheckoutPageAlwaysRendersCreateFormAndCompletedDocument() throws Exception {
        var issued = qr.issue(member.getEmail());
        qr.consume(issued.value(), branch.getId());
        checkins.checkOut(states.status(member.getEmail(), issued.id()).checkinId());
        mvc.perform(get("/member/qr-checkin").with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Đã checkout")))
                .andExpect(content().string(containsString("id=\"member-qr-create\"")))
                .andExpect(content().string(containsString("data-can-issue=\"true\"")))
                .andExpect(content().string(containsString("id=\"member-qr-retry\"")))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    void missingStatusModelCannotTruncateFunctionalAreaOfTemplate() throws Exception {
        mvc.perform(get("/member/test-qr-missing-status").with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Chưa đọc được trạng thái")))
                .andExpect(content().string(containsString("id=\"member-checkin-status\"")))
                .andExpect(content().string(containsString("id=\"member-qr-form\"")))
                .andExpect(content().string(containsString("id=\"member-qr-create\"")))
                .andExpect(content().string(containsString("id=\"member-qr-retry\"")))
                .andExpect(content().string(containsString("</html>")));
    }

    private User staff(UserRole role) {
        return users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("Test").lastName(role.name())
                .password("unused-test-hash").role(role).status(UserStatus.ACTIVE).enabled(true).tokenVersion(0).build());
    }

    private QrCheckinService.IssuedToken openAndIssueCheckout() {
        qr.consume(qr.issue(member.getEmail()).value(), branch.getId());
        return qr.issueCheckout(member.getEmail());
    }

    @Test
    void checkoutQrClosesSameRecordAndAllowsNextVisitWithoutAutomaticCheckoutOnIssue() {
        var issued = openAndIssueCheckout();
        assertTrue(issued.value().matches("UTEFITQO\\.[A-Za-z0-9_-]{43}"));
        var open = states.status(member.getEmail(), issued.id());
        assertEquals("OPEN", open.attendanceState());
        assertEquals("CHECK_OUT", open.qrPurpose());
        assertEquals("VALID", open.qrState());
        assertTrue(open.canCheckout());
        assertFalse(open.canIssue());
        assertEquals(120000, open.expiresAt() - open.serverTime());
        long count = checkins.history(member.getEmail(), null, null, null, 0).getTotalElements();
        var result = qr.consume(issued.value(), branch.getId());
        assertEquals(QrCheckinPurpose.CHECK_OUT, result.purpose());
        assertEquals(open.checkinId(), result.checkinId());
        assertNotNull(result.performedAt());
        assertEquals(count, checkins.history(member.getEmail(), null, null, null, 0).getTotalElements());
        assertEquals("CHECKED_OUT", states.status(member.getEmail(), issued.id()).attendanceState());
        assertEquals("UNAVAILABLE", states.status(member.getEmail(), issued.id()).qrState());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(issued.value(), branch.getId()));
        qr.consume(qr.issue(member.getEmail()).value(), branch.getId());
        assertEquals(count + 1, checkins.history(member.getEmail(), null, null, null, 0).getTotalElements());
    }

    @Test
    void checkinTokenNeverTurnsIntoCheckout() {
        var entry = qr.issue(member.getEmail());
        checkins.checkIn(member.getEmail(), branch.getId());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(entry.value(), branch.getId()));
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
        assertEquals(QrCheckinPurpose.CHECK_IN, qrTokens.findById(entry.id()).orElseThrow().getPurpose());
    }

    @Test
    void checkoutIssuanceWithoutOpenVisitIsRejectedAndRefreshRevokesOldCheckout() {
        assertThrows(IllegalArgumentException.class, () -> qr.issueCheckout(member.getEmail()));
        var old = openAndIssueCheckout();
        var refreshed = qr.issueCheckout(member.getEmail());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(old.value(), branch.getId()));
        qr.consume(refreshed.value(), branch.getId());
    }

    @Test
    void expiredCheckoutQrDoesNotCloseSession() {
        now.set(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusNanos(123456789));
        var issued = openAndIssueCheckout();
        now.set(now.get().plusSeconds(120));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(issued.value(), branch.getId()));
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
        assertEquals("EXPIRED", states.status(member.getEmail(), issued.id()).qrState());
    }

    @Test
    void wrongBranchDoesNotConsumeCheckoutQr() {
        var issued = openAndIssueCheckout();
        Branch other = branches.save(Branch.builder().name("Other checkout").address("Test").active(true).build());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(issued.value(), other.getId()));
        assertNull(qrTokens.findById(issued.id()).orElseThrow().getUsedAt());
        qr.consume(issued.value(), branch.getId());
    }

    @Test
    void oldCheckoutQrCannotCloseAnotherVisitAfterManualCheckout() {
        var issued = openAndIssueCheckout();
        checkins.checkOut(states.status(member.getEmail(), issued.id()).checkinId());
        checkins.checkIn(member.getEmail(), branch.getId());
        assertEquals("UNAVAILABLE", states.status(member.getEmail(), issued.id()).qrState());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(issued.value(), branch.getId()));
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
    }

    @Test
    void checkoutStillWorksWhenMembershipAndBranchExpireWhileExercising() {
        qr.consume(qr.issue(member.getEmail()).value(), branch.getId());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            memberships.findById(membership.getId()).orElseThrow().setEndDate(LocalDate.now().minusDays(1));
            branches.findById(branch.getId()).orElseThrow().setActive(false);
        });
        var issued = qr.issueCheckout(member.getEmail());
        qr.consume(issued.value(), branch.getId());
        assertFalse(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
    }

    @Test
    void lockedMemberRequiresManualCheckoutAndLogoutVersionInvalidatesCheckoutToken() {
        var issued = openAndIssueCheckout();
        new TransactionTemplate(transactions).executeWithoutResult(status -> users.findById(member.getId()).orElseThrow().setTokenVersion(1));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(issued.value(), branch.getId()));
        var refreshed = qr.issueCheckout(member.getEmail());
        new TransactionTemplate(transactions).executeWithoutResult(status -> users.findById(member.getId()).orElseThrow().setStatus(UserStatus.LOCKED));
        assertThrows(IllegalArgumentException.class, () -> qr.issueCheckout(member.getEmail()));
        assertThrows(IllegalArgumentException.class, () -> qr.consume(refreshed.value(), branch.getId()));
        checkins.checkOut(history.findByUser_IdOrderByCheckInAtDesc(member.getId()).get(0).getId());
        assertFalse(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
    }

    @Test
    void concurrentCheckoutScansCloseExactlyOneVisit() throws Exception {
        var issued = openAndIssueCheckout();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        Callable<Boolean> scan = () -> {
            gate.await();
            try { qr.consume(issued.value(), branch.getId()); return true; }
            catch (IllegalArgumentException ex) { return false; }
        };
        try {
            Future<Boolean> first = executor.submit(scan);
            Future<Boolean> second = executor.submit(scan);
            gate.countDown();
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, checkins.history(member.getEmail(), branch.getId(), null, false, 0).getTotalElements());
        } finally { executor.shutdownNow(); }
    }

    @Test
    void adminAndManagerMessagesIncludeActionMemberBranchAndTimeAndScopeIsEnforced() throws Exception {
        User admin = staff(UserRole.ADMIN);
        User manager = staff(UserRole.MANAGER);
        Branch other = branches.save(Branch.builder().name("Other manager checkout").address("Test").active(true).build());
        managerAccess.grant(admin.getEmail(), manager.getId(), other.getId());
        var issued = openAndIssueCheckout();
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> managerOperations.scan(manager.getEmail(), branch.getId(), issued.value()));
        assertThrows(IllegalArgumentException.class, () -> managerOperations.scan(manager.getEmail(), other.getId(), issued.value()));
        assertNull(qrTokens.findById(issued.id()).orElseThrow().getUsedAt());
        managerAccess.grant(admin.getEmail(), manager.getId(), branch.getId());
        mvc.perform(post("/manager/checkins/qr").with(user(manager.getEmail()).roles("MANAGER")).with(csrf())
                        .param("branchId", branch.getId().toString()).param("qrToken", issued.value()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("success", containsString("Check-out thành công")))
                .andExpect(flash().attribute("success", containsString(member.getFullName())))
                .andExpect(flash().attribute("success", containsString(branch.getName())));
        var entry = qr.issue(member.getEmail());
        mvc.perform(post("/admin/checkins/qr").with(user(admin.getEmail()).roles("ADMIN")).with(csrf())
                        .param("branchId", branch.getId().toString()).param("qrToken", entry.value()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("success", containsString("Check-in thành công")));
        var exit = qr.issueCheckout(member.getEmail());
        mvc.perform(post("/admin/checkins/qr").with(user(admin.getEmail()).roles("ADMIN")).with(csrf())
                        .param("branchId", branch.getId().toString()).param("qrToken", exit.value()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("success", containsString("Check-out thành công")));
    }

    @Test
    void checkoutEndpointRequiresMemberCsrfAndScannerPrivileges() throws Exception {
        var issued = openAndIssueCheckout();
        mvc.perform(post("/member/qr-checkin/checkout").with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/member/qr-checkin/checkout").with(user(member.getEmail()).roles("MEMBER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("qrImage", "qrTokenId"))
                .andExpect(content().string(containsString("QR CHECK-OUT")))
                .andExpect(content().string(containsString("</html>")));
        for (String role : List.of("PT", "ADMIN", "MANAGER")) {
            mvc.perform(post("/member/qr-checkin/checkout").with(user(member.getEmail()).roles(role)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/admin/checkins/qr").with(user(member.getEmail()).roles("MEMBER")).with(csrf())
                .param("branchId", branch.getId().toString()).param("qrToken", issued.value())).andExpect(status().isForbidden());
        mvc.perform(post("/manager/checkins/qr").with(user(member.getEmail()).roles("MEMBER")).with(csrf())
                .param("branchId", branch.getId().toString()).param("qrToken", issued.value())).andExpect(status().isForbidden());
    }

    @Test
    void managerRevocationAndInactiveAdminCannotConsumeCheckoutToken() {
        User admin = staff(UserRole.ADMIN);
        User manager = staff(UserRole.MANAGER);
        managerAccess.grant(admin.getEmail(), manager.getId(), branch.getId());
        var issued = openAndIssueCheckout();
        managerAccess.revoke(admin.getEmail(), manager.getId(), branch.getId());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> managerOperations.scan(manager.getEmail(), branch.getId(), issued.value()));
        new TransactionTemplate(transactions).executeWithoutResult(status -> users.findById(admin.getId()).orElseThrow().setEnabled(false));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> qr.consumeAsAdmin(admin.getEmail(), issued.value(), branch.getId()));
        assertNull(qrTokens.findById(issued.id()).orElseThrow().getUsedAt());
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
    }

    @Test
    void tokenForAnotherMembersVisitCannotCloseEitherSession() {
        var issued = openAndIssueCheckout();
        User other = staff(UserRole.MEMBER);
        Checkin otherVisit = history.save(Checkin.builder().user(other).branch(branch).checkInAt(LocalDateTime.now()).build());
        String corruptedValue = "UTEFITQO." + "C".repeat(43);
        String digest;
        try {
            digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(corruptedValue.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
        qrTokens.saveAndFlush(QrCheckinToken.builder().user(member).tokenHash(digest).accountVersion(0)
                .expiresAt(LocalDateTime.now(clock).plusSeconds(120)).purpose(QrCheckinPurpose.CHECK_OUT).checkin(otherVisit).build());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(corruptedValue, branch.getId()));
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(other.getId()));
        qr.consume(issued.value(), branch.getId());
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(other.getId()));
    }

    @Test
    void tamperedCheckoutPurposePrefixAndReadOnlyStatusCannotCloseSession() throws Exception {
        var issued = openAndIssueCheckout();
        String swapped = "UTEFITQR." + issued.value().substring("UTEFITQO.".length());
        assertThrows(IllegalArgumentException.class, () -> qr.consume(swapped, branch.getId()));
        mvc.perform(get("/member/qr-checkin/status").param("tokenId", issued.id().toString())
                        .with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.qrPurpose").value("CHECK_OUT"))
                .andExpect(jsonPath("$.canCheckout").value(true)).andExpect(jsonPath("$.attendanceState").value("OPEN"));
        assertNull(qrTokens.findById(issued.id()).orElseThrow().getUsedAt());
        assertTrue(history.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
    }
}
