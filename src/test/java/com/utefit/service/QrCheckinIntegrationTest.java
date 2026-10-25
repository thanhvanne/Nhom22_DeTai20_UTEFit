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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class QrCheckinIntegrationTest {
    @Autowired QrCheckinService qr;
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
}
