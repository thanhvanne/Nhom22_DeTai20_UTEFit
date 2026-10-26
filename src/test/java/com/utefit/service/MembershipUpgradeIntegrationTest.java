package com.utefit.service;

import com.utefit.config.SepayProperties;
import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MembershipUpgradeIntegrationTest {
    @Autowired MembershipPaymentService billing;
    @Autowired MembershipEligibilityService eligibility;
    @Autowired MemberClassBookingService booking;
    @Autowired AdminCheckinService checkins;
    @Autowired SepayPaymentService sepay;
    @Autowired SepayWebhookService webhook;
    @Autowired SepayManualReconciliationService manual;
    @Autowired MembershipBookingPolicyService policy;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired PaymentRepository payments;
    @Autowired SepayPaymentDetailsRepository details;
    @Autowired FitnessClassRepository classes;
    @Autowired ClassCategoryRepository categories;
    @Autowired MemberClassScheduleRepository schedules;
    @Autowired ClassRegistrationRepository registrations;
    @Autowired NotificationRepository notifications;
    @Autowired QrCheckinService qr;
    @Autowired jakarta.persistence.EntityManager manager;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired AdminMembershipPlanService adminPlans;
    @Autowired TrainerRepository trainers;
    @Autowired PersonalTrainingScheduleRepository personal;
    @MockitoBean SepayProperties properties;
    @MockitoBean SepayDatabaseGuard guard;
    private User member;
    private Branch home;
    private Branch other;
    private LocalDate today;

    @BeforeEach void setup() {
        today = LocalDate.now(SepayPaymentService.ZONE);
        member = user(UserRole.MEMBER);
        home = branches.save(Branch.builder().name("Membership H2 home " + UUID.randomUUID()).address("H2 only").active(true).build());
        other = branches.save(Branch.builder().name("Membership H2 other " + UUID.randomUUID()).address("H2 only").active(true).build());
        when(properties.mode()).thenReturn("TEST");
        when(properties.testMode()).thenReturn(true);
        when(properties.acceptingNewPayments()).thenReturn(true);
        when(properties.getBankCode()).thenReturn("TESTBANK");
        when(properties.getGateway()).thenReturn("TESTBANK");
        when(properties.getAccountNumber()).thenReturn("H2ACCOUNT");
        when(properties.getAccountName()).thenReturn("H2 fixture only");
        when(properties.getPaymentPrefix()).thenReturn("UTE");
        when(properties.getPaymentMinutes()).thenReturn(15);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private User user(UserRole role) {
        return users.save(User.builder().email(UUID.randomUUID() + "@membership.test").password("unused")
                .firstName("H2").lastName("Membership").role(role).status(UserStatus.ACTIVE).enabled(true).build());
    }
    private MembershipPlan plan(MembershipBenefit benefit, MembershipScope scope, Set<Long> branchIds) {
        return plans.save(MembershipPlan.builder().name("H2 plan " + UUID.randomUUID()).durationDays(30).price(new BigDecimal("100000"))
                .benefit(benefit).accessScope(scope).branchIds(new HashSet<>(scope == MembershipScope.SINGLE_BRANCH && branchIds.isEmpty() ? Set.of(home.getId()) : branchIds)).active(true).build());
    }
    private Long purchase(User owner, MembershipPlan plan, Branch branch) {
        var request = new MembershipPurchaseRequest(); request.setPlanId(plan.getId()); request.setBranchId(branch.getId());
        return billing.createMembership(owner.getEmail(), request);
    }
    private Membership payCash(User owner, Long id) {
        Long payment = billing.requestCashPayment(owner.getEmail(), id);
        billing.confirmCashPayment(payment);
        return memberships.findById(id).orElseThrow();
    }
    private Membership paid(MembershipPlan plan) {
        var configured = adminPlans.getPlanFormById(plan.getId());
        if (configured.getAccessScope() == MembershipScope.SINGLE_BRANCH && configured.getBranchIds().size() == 1)
            return payCash(member, purchase(member, plan, home));
        var legacy = memberships.save(Membership.builder().user(member).branch(home).plan(plan).quotedPrice(plan.getPrice())
                .quotedDurationDays(plan.getDurationDays()).quotedBenefit(plan.effectiveBenefit()).quotedScope(plan.effectiveScope())
                .branchIds(new HashSet<>(configured.getBranchIds())).status(MembershipStatus.ACTIVE).startDate(today).endDate(today.plusDays(plan.getDurationDays() - 1L)).build());
        payments.saveAndFlush(Payment.builder().user(member).membership(legacy).amount(plan.getPrice()).method(PaymentMethod.CASH).status(PaymentStatus.PAID).build());
        return legacy;
    }
    private ClassSchedule schedule(Branch branch, LocalDate day, MembershipBenefit benefit) {
        var category = categories.save(ClassCategory.builder().name("H2 category " + UUID.randomUUID()).active(true).requiredBenefit(benefit).build());
        var fitnessClass = classes.save(FitnessClass.builder().name("H2 class " + UUID.randomUUID()).branch(branch).category(category)
                .capacity(1).status(ClassStatus.OPEN).build());
        return schedules.save(ClassSchedule.builder().fitnessClass(fitnessClass).scheduleDate(day).startTime(LocalTime.of(10,0))
                .endTime(LocalTime.of(11,0)).room("H2").active(true).build());
    }
    private void execute(java.util.function.Consumer<jakarta.persistence.EntityManager> operation) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> operation.accept(manager));
    }

    @Test void calendarPeriodsQueueByBranchRegardlessOfPlanNameAndBenefit() {
        var monthly = plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of(home.getId()));
        monthly.setDurationUnit(MembershipDurationUnit.MONTH); monthly.setDurationQuantity(1);
        monthly.setBonusUnit(MembershipDurationUnit.DAY); monthly.setBonusQuantity(0); plans.saveAndFlush(monthly);
        var first = paid(monthly);
        var secondPlan = plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of(home.getId()));
        secondPlan.setDurationUnit(MembershipDurationUnit.WEEK); secondPlan.setDurationQuantity(2);
        secondPlan.setBonusUnit(MembershipDurationUnit.MONTH); secondPlan.setBonusQuantity(1); plans.saveAndFlush(secondPlan);
        Long pending = purchase(member, secondPlan, home);
        assertEquals(first.getEndDate(), billing.branchTimelines(member.getEmail()).get(0).totalEnd());
        var second = payCash(member, pending);
        assertEquals(first.getEndDate().plusDays(1), second.getStartDate());
        assertEquals(second.getStartDate().plusWeeks(2).plusMonths(1).minusDays(1), second.getEndDate());
        var thirdPlan = plan(MembershipBenefit.YOGA, MembershipScope.SINGLE_BRANCH, Set.of(home.getId()));
        thirdPlan.setDurationUnit(MembershipDurationUnit.MONTH); thirdPlan.setDurationQuantity(3);
        thirdPlan.setBonusUnit(MembershipDurationUnit.WEEK); thirdPlan.setBonusQuantity(2); plans.saveAndFlush(thirdPlan);
        var third = paid(thirdPlan);
        assertEquals(second.getEndDate().plusDays(1), third.getStartDate());
        assertEquals(third.getStartDate().plusMonths(3).plusWeeks(2).minusDays(1), third.getEndDate());
        var branchB = plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of(other.getId()));
        var independent = payCash(member, purchase(member, branchB, other));
        assertEquals(today, independent.getStartDate());
        assertTrue(eligibility.canUseBranch(member.getId(), home.getId(), today));
        assertTrue(eligibility.canUseBranch(member.getId(), other.getId(), today));
        assertFalse(eligibility.canUse(member.getId(), home.getId(), today, MembershipBenefit.PT));
        var futureClass = schedule(home, second.getStartDate(), MembershipBenefit.PT);
        booking.book(member.getEmail(), futureClass.getId());
        assertFalse(eligibility.canUse(member.getId(), home.getId(), second.getEndDate().plusDays(1), MembershipBenefit.PT));
        var history = billing.branchTimelines(member.getEmail());
        assertEquals(2, history.size());
        assertEquals(third.getEndDate(), history.stream().filter(branch -> branch.branchId().equals(home.getId())).findFirst().orElseThrow().totalEnd());
    }

    @Test void expiredBranchDoesNotInvalidateAnotherBranchAndNewChainRestartsToday() {
        var first = paid(plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of(home.getId())));
        payCash(member, purchase(member, plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of(other.getId())), other));
        execute(entityManager -> {
            var expired = entityManager.find(Membership.class, first.getId());
            expired.setStartDate(today.minusDays(30)); expired.setEndDate(today.minusDays(1)); expired.setStatus(MembershipStatus.EXPIRED);
        });
        assertFalse(eligibility.canUseBranch(member.getId(), home.getId(), today));
        assertTrue(eligibility.canUseBranch(member.getId(), other.getId(), today));
        assertThrows(IllegalArgumentException.class, () -> checkins.checkIn(member.getEmail(), home.getId()));
        checkins.checkIn(member.getEmail(), other.getId());
        var resumed = paid(plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of(home.getId())));
        assertEquals(today, resumed.getStartDate());
    }

    @Test void concurrentCalendarPaymentsUseLockedBranchChainAndKeepEachSnapshot() throws Exception {
        var first = paid(plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of(home.getId())));
        var monthly = plan(MembershipBenefit.YOGA, MembershipScope.SINGLE_BRANCH, Set.of(home.getId()));
        monthly.setDurationUnit(MembershipDurationUnit.MONTH); monthly.setDurationQuantity(2);
        monthly.setBonusUnit(MembershipDurationUnit.WEEK); monthly.setBonusQuantity(1); plans.saveAndFlush(monthly);
        var yearly = plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of(home.getId()));
        yearly.setDurationUnit(MembershipDurationUnit.YEAR); yearly.setDurationQuantity(1);
        yearly.setBonusUnit(MembershipDurationUnit.MONTH); yearly.setBonusQuantity(1); plans.saveAndFlush(yearly);
        Long one = purchase(member, monthly, home); Long two = purchase(member, yearly, home);
        Long onePayment = billing.requestCashPayment(member.getEmail(), one); Long twoPayment = billing.requestCashPayment(member.getEmail(), two);
        var pool = Executors.newFixedThreadPool(2); var latch = new CountDownLatch(1);
        try {
            var firstTask = pool.submit(() -> { latch.await(); billing.confirmCashPayment(onePayment); return true; });
            var secondTask = pool.submit(() -> { latch.await(); billing.confirmCashPayment(twoPayment); return true; }); latch.countDown();
            assertTrue(firstTask.get(30, TimeUnit.SECONDS)); assertTrue(secondTask.get(30, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        var periods = List.of(memberships.findById(one).orElseThrow(), memberships.findById(two).orElseThrow()).stream().sorted(Comparator.comparing(Membership::getStartDate)).toList();
        assertEquals(first.getEndDate().plusDays(1), periods.get(0).getStartDate());
        assertEquals(periods.get(0).getEndDate().plusDays(1), periods.get(1).getStartDate());
        periods.forEach(period -> assertEquals(period.term().endDate(period.getStartDate()), period.getEndDate()));
        LocalDate last = periods.get(1).getEndDate();
        assertThrows(IllegalArgumentException.class, () -> billing.confirmCashPayment(onePayment));
        assertEquals(last, billing.branchTimelines(member.getEmail()).get(0).totalEnd());
    }

    @Test void newPurchasesCannotUseLegacyFlexButPreviouslyPaidRightsRemain() {
        var legacyPlan = plan(MembershipBenefit.PT, MembershipScope.MULTI_BRANCH, Set.of(home.getId(), other.getId()));
        var legacy = paid(legacyPlan);
        assertThrows(IllegalArgumentException.class, () -> purchase(member, legacyPlan, home));
        execute(entityManager -> {
            var changed = entityManager.find(MembershipPlan.class, legacyPlan.getId());
            changed.setAccessScope(MembershipScope.SINGLE_BRANCH); changed.getBranchIds().clear(); changed.getBranchIds().add(home.getId());
            changed.setDurationUnit(MembershipDurationUnit.YEAR); changed.setDurationQuantity(2);
            changed.setBonusUnit(MembershipDurationUnit.MONTH); changed.setBonusQuantity(1);
        });
        var unchanged = memberships.findById(legacy.getId()).orElseThrow();
        assertNull(unchanged.getQuotedDurationUnit());
        assertEquals(30, unchanged.term().daysAt(today));
        assertTrue(eligibility.canUse(member.getId(), other.getId(), today, MembershipBenefit.PT));
        var newAtOther = plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of(other.getId()));
        var next = payCash(member, purchase(member, newAtOther, other));
        assertEquals(legacy.getEndDate().plusDays(1), next.getStartDate());
    }

    @Test void renewalPreservesPaidDaysAndPendingDoesNotExtend() {
        var plan = plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of());
        var first = paid(plan);
        Long renewal = billing.renew(member.getEmail(), first.getId());
        assertEquals(first.getEndDate(), memberships.findById(first.getId()).orElseThrow().getEndDate());
        assertNull(memberships.findById(renewal).orElseThrow().getEndDate());
        assertFalse(eligibility.canUseBranch(member.getId(), home.getId(), first.getEndDate().plusDays(1)));
        var second = payCash(member, renewal);
        assertEquals(first.getEndDate().plusDays(1), second.getStartDate());
        assertEquals(first.getEndDate().plusDays(30), second.getEndDate());
        assertEquals(first.getId(), second.getRenewalOf().getId());
        assertEquals(second.getEndDate(), billing.myMemberships(member.getEmail()).get(0).paidThrough());
    }

    @Test void renewingExpiredMembershipStartsToday() {
        var first = paid(plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()));
        execute(entityManager -> entityManager.createNativeQuery("UPDATE memberships SET status = 'EXPIRED', start_date = :start, end_date = :end WHERE id = :id")
                .setParameter("start", today.minusDays(40)).setParameter("end", today.minusDays(11)).setParameter("id", first.getId()).executeUpdate());
        var renewal = payCash(member, billing.renew(member.getEmail(), first.getId()));
        assertEquals(today, renewal.getStartDate());
        assertEquals(today.plusDays(29), renewal.getEndDate());
    }

    @Test void concurrentCashRenewalsAppendSixtyDaysWithoutOverlap() throws Exception {
        var first = paid(plan(MembershipBenefit.GYM, MembershipScope.ALL_BRANCHES, Set.of()));
        Long firstOrder = purchase(member, plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()), home);
        Long secondOrder = purchase(member, plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()), home);
        Long firstPayment = billing.requestCashPayment(member.getEmail(), firstOrder);
        Long secondPayment = billing.requestCashPayment(member.getEmail(), secondOrder);
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var one = pool.submit(() -> { start.await(); billing.confirmCashPayment(firstPayment); return true; });
            var two = pool.submit(() -> { start.await(); billing.confirmCashPayment(secondPayment); return true; }); start.countDown();
            assertTrue(one.get(20, TimeUnit.SECONDS)); assertTrue(two.get(20, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        var periods = List.of(memberships.findById(firstOrder).orElseThrow(), memberships.findById(secondOrder).orElseThrow()).stream()
                .sorted(Comparator.comparing(Membership::getStartDate)).toList();
        assertEquals(first.getEndDate().plusDays(1), periods.get(0).getStartDate());
        assertEquals(periods.get(0).getEndDate().plusDays(1), periods.get(1).getStartDate());
        assertEquals(first.getEndDate().plusDays(60), periods.get(1).getEndDate());
    }

    @Test void futurePtClassRequiresPaidCoverageAtClassDate() {
        var first = paid(plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of()));
        var session = schedule(home, first.getEndDate().plusDays(5), MembershipBenefit.PT);
        Long renewal = billing.renew(member.getEmail(), first.getId());
        assertThrows(IllegalArgumentException.class, () -> booking.book(member.getEmail(), session.getId()));
        payCash(member, renewal);
        booking.book(member.getEmail(), session.getId());
        assertEquals(1, registrations.countBySchedule_IdAndStatus(session.getId(), RegistrationStatus.REGISTERED));
    }

    @Test void sameBranchBenefitsQueueWhileOtherBranchesRemainIndependent() {
        var gym = paid(plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()));
        var yoga = paid(plan(MembershipBenefit.YOGA, MembershipScope.SINGLE_BRANCH, Set.of()));
        assertEquals(gym.getEndDate().plusDays(1), yoga.getStartDate());
        assertFalse(eligibility.canUse(member.getId(), home.getId(), today, MembershipBenefit.YOGA));
        assertTrue(eligibility.canUse(member.getId(), home.getId(), today, MembershipBenefit.GYM));
        booking.book(member.getEmail(), schedule(home, yoga.getStartDate(), MembershipBenefit.YOGA).getId());
        assertFalse(eligibility.canUse(member.getId(), home.getId(), today, MembershipBenefit.PT));
        assertFalse(eligibility.canUseBranch(member.getId(), other.getId(), today));
        assertThrows(IllegalArgumentException.class, () -> checkins.checkIn(member.getEmail(), other.getId()));
        var wrongType = schedule(home, today.plusDays(1), MembershipBenefit.PT);
        assertThrows(IllegalArgumentException.class, () -> booking.book(member.getEmail(), wrongType.getId()));
        var wrongBranch = schedule(other, today.plusDays(2), MembershipBenefit.GYM);
        assertThrows(IllegalArgumentException.class, () -> booking.book(member.getEmail(), wrongBranch.getId()));
    }

    @Test void allBranchesAndMultiBranchRespectActiveBranchesAndCheckin() {
        paid(plan(MembershipBenefit.PT, MembershipScope.MULTI_BRANCH, Set.of(home.getId(), other.getId())));
        assertTrue(eligibility.canUseBranch(member.getId(), other.getId(), today));
        checkins.checkIn(member.getEmail(), other.getId());
        var third = branches.save(Branch.builder().name("H2 third").address("H2").active(true).build());
        assertFalse(eligibility.canUseBranch(member.getId(), third.getId(), today));
        paid(plan(MembershipBenefit.GYM, MembershipScope.ALL_BRANCHES, Set.of()));
        assertTrue(eligibility.canUseBranch(member.getId(), third.getId(), today));
        assertFalse(eligibility.canUse(member.getId(), third.getId(), today, MembershipBenefit.PT));
        assertThrows(IllegalArgumentException.class, () -> booking.book(member.getEmail(), schedule(third, today.plusDays(4), MembershipBenefit.PT).getId()));
        booking.book(member.getEmail(), schedule(third, today.plusDays(3), MembershipBenefit.GYM).getId());
        third.setActive(false); branches.saveAndFlush(third);
        assertFalse(eligibility.canUseBranch(member.getId(), third.getId(), today));
    }

    @Test void planEditsDoNotChangePurchasedRightsSnapshot() {
        var product = plan(MembershipBenefit.YOGA, MembershipScope.MULTI_BRANCH, Set.of(home.getId(), other.getId()));
        var original = paid(product);
        execute(entityManager -> {
            var changed = entityManager.find(MembershipPlan.class, product.getId());
            changed.setBenefit(MembershipBenefit.PT); changed.setAccessScope(MembershipScope.SINGLE_BRANCH); changed.getBranchIds().clear();
        });
        assertTrue(eligibility.canUse(member.getId(), other.getId(), today, MembershipBenefit.YOGA));
        assertFalse(eligibility.canUse(member.getId(), other.getId(), today, MembershipBenefit.PT));
        assertThrows(IllegalArgumentException.class, () -> billing.renew(member.getEmail(), original.getId()));
    }

    private SepayWebhookPayload payload(Long paymentId, long transactionId) {
        return payload(paymentId, transactionId, "100000");
    }
    private SepayWebhookPayload payload(Long paymentId, long transactionId, String amount) {
        var order = details.findById(paymentId).orElseThrow();
        String body = "{\"id\":" + transactionId + ",\"gateway\":\"TESTBANK\",\"accountNumber\":\"H2ACCOUNT\",\"code\":\""
                + order.getPaymentReference() + "\",\"content\":\"" + order.getPaymentReference()
                + "\",\"transferType\":\"in\",\"transferAmount\":" + amount + ",\"referenceCode\":\"H2REF" + transactionId
                + "\",\"transactionDate\":\"" + LocalDateTime.now(SepayPaymentService.ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\"}";
        return SepayWebhookPayload.parse(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test void sepayDuplicateWebhookAddsRenewalDaysOnlyOnce() {
        var original = paid(plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()));
        Long renewal = billing.renew(member.getEmail(), original.getId());
        Long payment = sepay.start(member.getEmail(), renewal);
        var incoming = payload(payment, 200000000L + payment);
        webhook.accept(incoming); webhook.accept(incoming);
        assertEquals(original.getEndDate().plusDays(30), memberships.findById(renewal).orElseThrow().getEndDate());
        assertEquals(PaymentStatus.PAID, payments.findById(payment).orElseThrow().getStatus());
    }

    @Test void manualReconciliationRenewalAndConfirmedRefundRevokeFutureClassOnly() {
        var original = paid(plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of()));
        when(properties.mode()).thenReturn("LIVE"); when(properties.testMode()).thenReturn(false);
        Long renewal = billing.renew(member.getEmail(), original.getId());
        Long payment = sepay.start(member.getEmail(), renewal);
        User admin = user(UserRole.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getEmail(), "unused",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        ReflectionTestUtils.setField(manual, "enabled", true);
        String bankReference = "H2BANK" + payment;
        manual.confirmReceived(payment, bankReference, true);
        var following = paid(plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of(home.getId())));
        LocalDate followingStart = following.getStartDate(); LocalDate followingEnd = following.getEndDate();
        var session = schedule(home, original.getEndDate().plusDays(5), MembershipBenefit.PT);
        booking.book(member.getEmail(), session.getId());
        long count = notifications.count();
        var recordId = contextRecord(payment);
        manual.confirmRefund(payment, recordId, new SepayManualReconciliationService.RefundEvidence("H2REFUND" + payment,
                new BigDecimal("100000"), LocalDateTime.now(SepayPaymentService.ZONE), "OFFICIAL_BANK_STATEMENT",
                "H2 only: official receiving bank outgoing statement completed, reference documented.",
                "H2 internal verification matches original sender and refund beneficiary.", "H2 full renewal refund was actually completed and independently verified.", true));
        assertEquals(PaymentStatus.REFUNDED, payments.findById(payment).orElseThrow().getStatus());
        assertEquals(followingStart, memberships.findById(following.getId()).orElseThrow().getStartDate());
        assertEquals(followingEnd, memberships.findById(following.getId()).orElseThrow().getEndDate());
        assertTrue(billing.branchTimelines(member.getEmail()).get(0).gap());
        assertEquals(RegistrationStatus.CANCELLED, registrations.findByUser_IdAndSchedule_Id(member.getId(), session.getId()).orElseThrow().getStatus());
        assertEquals(original.getEndDate(), memberships.findById(original.getId()).orElseThrow().getEndDate());
        assertTrue(notifications.count() > count);
    }
    @Autowired SepayBankReconciliationRepository audit;
    private Long contextRecord(Long payment) { return audit.findByPayment_IdOrderByIdDesc(payment).get(0).getId(); }

    @Test void concurrentSepayRenewalsAppendWithoutLosingDays() throws Exception {
        var first = paid(plan(MembershipBenefit.GYM, MembershipScope.ALL_BRANCHES, Set.of()));
        Long firstPayment = sepay.start(member.getEmail(), purchase(member, plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()), home));
        Long secondPayment = sepay.start(member.getEmail(), purchase(member, plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()), home));
        var firstReceipt = payload(firstPayment, 210000000L + firstPayment);
        var secondReceipt = payload(secondPayment, 210000000L + secondPayment);
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var one = pool.submit(() -> { start.await(); webhook.accept(firstReceipt); return true; });
            var two = pool.submit(() -> { start.await(); webhook.accept(secondReceipt); return true; }); start.countDown();
            assertTrue(one.get(20, TimeUnit.SECONDS)); assertTrue(two.get(20, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        assertEquals(first.getEndDate().plusDays(60), billing.myMemberships(member.getEmail()).get(0).paidThrough());
        assertEquals(3, memberships.findPaidAccessByUser(member.getId()).size());
    }

    @Test void reviewOrFailedPaymentDoesNotBuyAccess() {
        var first = paid(plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()));
        Long renewal = billing.renew(member.getEmail(), first.getId());
        Long payment = sepay.start(member.getEmail(), renewal);
        webhook.accept(payload(payment, 220000000L + payment, "99999"));
        assertEquals(PaymentStatus.PENDING, payments.findById(payment).orElseThrow().getStatus());
        assertNull(memberships.findById(renewal).orElseThrow().getEndDate());
        assertFalse(eligibility.canUseBranch(member.getId(), home.getId(), first.getEndDate().plusDays(1)));
        Long otherOrder = purchase(member, plan(MembershipBenefit.GYM, MembershipScope.SINGLE_BRANCH, Set.of()), home);
        Long cash = billing.requestCashPayment(member.getEmail(), otherOrder);
        billing.cancelPendingMembership(member.getEmail(), otherOrder);
        assertEquals(PaymentStatus.CANCELLED, payments.findById(cash).orElseThrow().getStatus());
        assertFalse(eligibility.canUseBranch(member.getId(), home.getId(), first.getEndDate().plusDays(1)));
    }

    @Test void classCapacityIsSafeUnderConcurrentMembers() throws Exception {
        var product = plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of(home.getId()));
        paid(product);
        User second = user(UserRole.MEMBER);
        payCash(second, purchase(second, product, home));
        var session = schedule(home, today.plusDays(1), MembershipBenefit.PT);
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var one = pool.submit(() -> { start.await(); try { booking.book(member.getEmail(), session.getId()); return true; } catch (IllegalArgumentException ex) { return false; } });
            var two = pool.submit(() -> { start.await(); try { booking.book(second.getEmail(), session.getId()); return true; } catch (IllegalArgumentException ex) { return false; } }); start.countDown();
            assertNotEquals(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        assertEquals(1, registrations.countBySchedule_IdAndStatus(session.getId(), RegistrationStatus.REGISTERED));
    }

    @Test void cancellationRevalidationPreservesHistoryAndNotifiesOnlyOnce() {
        var first = paid(plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of()));
        var next = payCash(member, billing.renew(member.getEmail(), first.getId()));
        var session = schedule(home, first.getEndDate().plusDays(2), MembershipBenefit.PT);
        booking.book(member.getEmail(), session.getId());
        execute(entityManager -> entityManager.find(Membership.class, next.getId()).setStatus(MembershipStatus.CANCELLED));
        assertEquals(1, policy.revalidateMember(member.getId()));
        long count = notifications.count();
        assertEquals(0, policy.revalidateMember(member.getId()));
        assertEquals(count, notifications.count());
        assertTrue(payments.existsByMembership_IdAndStatus(next.getId(), PaymentStatus.PAID));
        assertEquals(first.getEndDate().plusDays(30), memberships.findById(next.getId()).orElseThrow().getEndDate());
    }

    @Test void qrAllBranchesChecksOutOnlyAtOriginalBranch() {
        paid(plan(MembershipBenefit.GYM, MembershipScope.ALL_BRANCHES, Set.of()));
        User admin = user(UserRole.ADMIN);
        var entry = qr.issue(member.getEmail());
        var entered = qr.consumeAsAdmin(admin.getEmail(), entry.value(), other.getId());
        assertEquals(QrCheckinPurpose.CHECK_IN, entered.purpose());
        var exit = qr.issueCheckout(member.getEmail());
        assertThrows(IllegalArgumentException.class, () -> qr.consumeAsAdmin(admin.getEmail(), exit.value(), home.getId()));
        var exited = qr.consumeAsAdmin(admin.getEmail(), exit.value(), other.getId());
        assertEquals(QrCheckinPurpose.CHECK_OUT, exited.purpose());
        assertEquals(entered.checkinId(), exited.checkinId());
    }

    @Test void alternatePaidBenefitKeepsFutureBookingAfterOnePeriodIsCancelled() {
        var first = paid(plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of()));
        var renewal = payCash(member, billing.renew(member.getEmail(), first.getId()));
        var backup = plan(MembershipBenefit.ALL, MembershipScope.ALL_BRANCHES, Set.of());
        backup.setDurationDays(90); plans.saveAndFlush(backup); paid(backup);
        var session = schedule(home, first.getEndDate().plusDays(2), MembershipBenefit.PT);
        booking.book(member.getEmail(), session.getId());
        execute(entityManager -> entityManager.find(Membership.class, renewal.getId()).setStatus(MembershipStatus.CANCELLED));
        assertEquals(0, policy.revalidateMember(member.getId()));
        assertEquals(RegistrationStatus.REGISTERED, registrations.findByUser_IdAndSchedule_Id(member.getId(), session.getId()).orElseThrow().getStatus());
    }

    @Test void adminBranchConfigurationRejectsInvalidScopesAndKeepsSnapshot() {
        var form = com.utefit.dto.admin.MembershipPlanFormDto.builder().name("H2 configured " + UUID.randomUUID())
                .durationDays(30).price(new BigDecimal("100000")).benefit(MembershipBenefit.YOGA)
                .accessScope(MembershipScope.MULTI_BRANCH).build();
        assertThrows(IllegalArgumentException.class, () -> adminPlans.createPlan(form));
        form.setBranchIds(Set.of(home.getId(), other.getId()));
        assertThrows(IllegalArgumentException.class, () -> adminPlans.createPlan(form));
        form.setAccessScope(MembershipScope.SINGLE_BRANCH);
        form.setBranchIds(Set.of(home.getId()));
        adminPlans.createPlan(form);
        var created = plans.findByActiveTrueOrderByNameAsc().stream().filter(candidate -> candidate.getName().equals(form.getName())).findFirst().orElseThrow();
        paid(created);
        form.setAccessScope(MembershipScope.ALL_BRANCHES);
        assertThrows(IllegalArgumentException.class, () -> adminPlans.updatePlan(created.getId(), form));
        form.setAccessScope(MembershipScope.SINGLE_BRANCH);
        form.setBranchIds(Set.of(other.getId()));
        adminPlans.updatePlan(created.getId(), form);
        assertTrue(eligibility.canUse(member.getId(), home.getId(), today, MembershipBenefit.YOGA));
        assertFalse(eligibility.canUse(member.getId(), other.getId(), today, MembershipBenefit.YOGA));
    }

    @Test void memberAndAdminPagesRenderNewRightsAndRenewalPreview() throws Exception {
        var first = paid(plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of(home.getId())));
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/member/memberships")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(member.getEmail()).roles("MEMBER")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Mua thêm gói này")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(first.getEndDate().plusDays(30).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        User admin = user(UserRole.ADMIN);
        for (String path : List.of("/admin/membership-plans/create", "/admin/categories/create", "/admin/membership-plans")) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN")))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        }
    }

    @Test void revokedRenewalCancelsFuturePersonalTrainingAndNotifiesMemberAndPt() {
        var original = paid(plan(MembershipBenefit.PT, MembershipScope.SINGLE_BRANCH, Set.of(home.getId())));
        var renewal = payCash(member, billing.renew(member.getEmail(), original.getId()));
        User trainerUser = user(UserRole.PT);
        var trainer = trainers.save(Trainer.builder().user(trainerUser).branch(home).active(true).build());
        var session = personal.save(PersonalTrainingSchedule.builder().trainer(trainer).member(member).branch(home)
                .scheduleDate(original.getEndDate().plusDays(2)).startTime(LocalTime.of(14,0)).endTime(LocalTime.of(15,0))
                .status(PersonalTrainingStatus.SCHEDULED).active(true).build());
        assertEquals(0, policy.revalidateMember(member.getId()));
        execute(entityManager -> entityManager.find(Membership.class, renewal.getId()).setStatus(MembershipStatus.CANCELLED));
        long before = notifications.count();
        assertEquals(1, policy.revalidateMember(member.getId()));
        assertFalse(personal.findById(session.getId()).orElseThrow().getActive());
        assertEquals(PersonalTrainingStatus.CANCELLED, personal.findById(session.getId()).orElseThrow().getStatus());
        assertEquals(before + 2, notifications.count());
    }
}
