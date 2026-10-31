package com.utefit.service;

import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "utefit.loyalty.enabled=true")
@Transactional
class MemberLoyaltyIntegrationTest {
    @Autowired MembershipPaymentService billing;
    @Autowired MemberLoyaltyService loyalty;
    @Autowired MembershipOfferService offers;
    @Autowired UserRepository users;
    @Autowired MembershipRepository memberships;
    @Autowired PaymentRepository payments;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired PromotionRepository promotions;
    @Autowired LoyaltyPaymentExclusionRepository exclusions;
    @Autowired WebApplicationContext context;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private record Fixture(User user, Branch branch) {}
    private Fixture fixture() {
        String identity = UUID.randomUUID().toString();
        User member = users.saveAndFlush(User.builder().email(identity + "@loyalty.test").firstName("Loyalty").lastName("QA").password("unused").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        Branch branch = branches.saveAndFlush(Branch.builder().name(identity).address("Isolated H2").active(true).build());
        return new Fixture(member, branch);
    }

    private MembershipPurchaseRequest request(Fixture fixture, String amount, String selection) {
        MembershipPlan plan = plans.saveAndFlush(MembershipPlan.builder().name(UUID.randomUUID().toString()).price(new BigDecimal(amount)).durationDays(210).active(true).branchIds(Set.of(fixture.branch().getId())).build());
        var request = new MembershipPurchaseRequest();
        request.setBranchId(fixture.branch().getId()); request.setPlanId(plan.getId()); request.setOfferSelection(selection);
        return request;
    }

    private Payment paid(Fixture fixture, String amount) {
        Long order = billing.createMembership(fixture.user().getEmail(), request(fixture, amount, "NONE"));
        Long payment = billing.requestCashPayment(fixture.user().getEmail(), order);
        billing.confirmCashPayment(payment);
        return payments.findById(payment).orElseThrow();
    }

    private void amount(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }

    @Test void newMemberReceivesWelcomeOnceAndPendingReservationCannotBeMultiplied() {
        var fixture = fixture(); var input = request(fixture, "200000", "AUTO");
        var preview = billing.quote(fixture.user().getEmail(), input);
        assertEquals("WELCOME", preview.kind()); amount("20000", preview.discount()); amount("180000", preview.finalPrice());
        Long order = billing.createMembership(fixture.user().getEmail(), input);
        assertEquals(order, billing.createMembership(fixture.user().getEmail(), input));
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(fixture.user().getEmail(), request(fixture, "300000", "AUTO")));
        billing.confirmCashPayment(billing.requestCashPayment(fixture.user().getEmail(), order));
        assertFalse(loyalty.summary(fixture.user().getEmail()).newCustomer());
        assertEquals("NONE", billing.quote(fixture.user().getEmail(), request(fixture, "200000", "AUTO")).kind());
    }

    @Test void offerServiceCannotRepriceAnAlreadyPersistedOrder() {
        var fixture = fixture(); var input = request(fixture, "200000", "AUTO");
        Long id = billing.createMembership(fixture.user().getEmail(), input);
        var order = memberships.findById(id).orElseThrow();
        assertThrows(IllegalStateException.class, () -> offers.apply(fixture.user(), order, input));
        amount("180000", order.getQuotedPrice());
    }

    @Test void cancellationBeforeAnyMoneyReleasesWelcomeButRefundNeverMakesCustomerNewAgain() {
        var fixture = fixture(); var input = request(fixture, "200000", "AUTO");
        Long order = billing.createMembership(fixture.user().getEmail(), input);
        assertTrue(billing.cancelPendingMembership(fixture.user().getEmail(), order));
        assertTrue(loyalty.summary(fixture.user().getEmail()).welcomeAvailable());
        var payment = paid(fixture, "200000");
        payment.setStatus(PaymentStatus.REFUNDED); payment.getMembership().setStatus(MembershipStatus.CANCELLED); payments.flush();
        var summary = loyalty.summary(fixture.user().getEmail());
        amount("0", summary.spending()); assertFalse(summary.newCustomer()); assertFalse(summary.welcomeAvailable());
    }

    @Test void paidHistoryImmediatelyDeterminesSilverGoldAndNeverRepricesExistingPendingOrder() {
        var fixture = fixture(); paid(fixture, "500000");
        assertEquals("SILVER", loyalty.summary(fixture.user().getEmail()).tier());
        var input = request(fixture, "400000", "AUTO");
        Long order = billing.createMembership(fixture.user().getEmail(), input);
        amount("380000", memberships.findById(order).orElseThrow().getQuotedPrice());
        paid(fixture, "1000000");
        var summary = loyalty.summary(fixture.user().getEmail());
        assertEquals("GOLD", summary.tier()); assertNull(summary.nextTier()); amount("1500000", summary.spending());
        assertEquals(order, billing.createMembership(fixture.user().getEmail(), input));
        amount("380000", memberships.findById(order).orElseThrow().getQuotedPrice());
        amount("380000", billing.quote(fixture.user().getEmail(), input).finalPrice());
        amount("360000", billing.quote(fixture.user().getEmail(), request(fixture, "400000", "AUTO")).finalPrice());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"499999,BRONZE", "500000,SILVER", "1499999,SILVER", "1500000,GOLD"})
    void exactTierBoundariesUseNetConfirmedSpending(String spending, String tier) {
        var fixture = fixture(); paid(fixture, spending);
        assertEquals(tier, loyalty.summary(fixture.user().getEmail()).tier());
    }

    @Test void pendingFailedCancelledAndRefundedDoNotContributeButAuditedPaidCashDoes() {
        var fixture = fixture(); var input = request(fixture, "500000", "NONE");
        Long order = billing.createMembership(fixture.user().getEmail(), input);
        Long paymentId = billing.requestCashPayment(fixture.user().getEmail(), order);
        var payment = payments.findById(paymentId).orElseThrow();
        for (var status : List.of(PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.REFUNDED)) {
            payment.setStatus(status); payments.flush(); amount("0", loyalty.summary(fixture.user().getEmail()).spending());
        }
        payment.setStatus(PaymentStatus.PENDING); billing.confirmCashPayment(paymentId);
        amount("500000", loyalty.summary(fixture.user().getEmail()).spending());
        exclusions.saveAndFlush(LoyaltyPaymentExclusion.builder().paymentId(paymentId).reason("Isolated reviewed demo").reviewedAt(LocalDateTime.now()).build());
        amount("500000", loyalty.summary(fixture.user().getEmail()).spending());
        assertEquals("SILVER", loyalty.summary(fixture.user().getEmail()).tier());
        assertTrue(exclusions.existsById(paymentId));
    }

    @Test void seedSignatureNeverExcludesPaidCashFromTierButKeepsPromotionHistorySeparate() {
        var fixture = fixture(); fixture.user().setEmail("member02@utefit.test"); users.flush();
        var payment = paid(fixture, "500000");
        payment.setTransactionCode("DEMO-CASH-M02A"); payments.flush();
        assertTrue(MemberLoyaltyService.isSeedCash(payment)); amount("500000", loyalty.summary(fixture.user().getEmail()).spending());
        assertTrue(loyalty.summary(fixture.user().getEmail()).newCustomer());
        assertTrue(loyalty.history(fixture.user()).paidMemberships().isEmpty());
        payment.setTransactionCode("CASH-" + payment.getId()); payments.flush();
        assertFalse(MemberLoyaltyService.isSeedCash(payment)); amount("500000", loyalty.summary(fixture.user().getEmail()).spending());
        assertFalse(loyalty.summary(fixture.user().getEmail()).newCustomer());
    }

    private Payment historicalCash(Fixture fixture, String amount, String code, PaymentStatus status, MembershipStatus membershipStatus) {
        var input = request(fixture, amount, "NONE");
        LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        var order = memberships.saveAndFlush(Membership.builder().user(fixture.user()).branch(fixture.branch())
                .plan(plans.findById(input.getPlanId()).orElseThrow()).status(membershipStatus)
                .quotedPrice(new BigDecimal(amount)).quotedDurationDays(210).startDate(today.minusMonths(7))
                .endDate(membershipStatus == MembershipStatus.EXPIRED ? today.minusDays(1) : today.plusDays(1)).build());
        return payments.saveAndFlush(Payment.builder().user(fixture.user()).membership(order).method(PaymentMethod.CASH)
                .status(status).amount(new BigDecimal(amount)).transactionCode(code)
                .paidAt(status == PaymentStatus.PENDING ? null : today.minusMonths(7).atStartOfDay()).build());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "2,M02A,300000,PAID,ACTIVE,300000,BRONZE",
            "3,M03A,300000,PAID,EXPIRED,300000,BRONZE",
            "4,M04A|M04NEXT,300000,PAID,ACTIVE,600000,SILVER",
            "5,M05A|M05B,300000,PAID,ACTIVE,600000,SILVER",
            "6,M06A,300000,PENDING,PENDING,0,BRONZE",
            "19,M19,2800000,PAID,ACTIVE,2800000,GOLD",
            "20,M20,5600000,PAID,ACTIVE,5600000,GOLD"
    })
    void seededCashHistoryDeterminesTierOnProfileWithoutAnyNewPurchase(int number, String keys, String amount,
            PaymentStatus status, MembershipStatus membershipStatus, String total, String tier) throws Exception {
        var fixture = fixture(); fixture.user().setEmail(String.format(Locale.ROOT, "member%02d@utefit.test", number)); users.flush();
        for (String key : keys.split("\\|")) historicalCash(fixture, amount, "DEMO-CASH-" + key, status, membershipStatus);
        long paymentCount = payments.count(); long membershipCount = memberships.count();
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var result = mvc.perform(get("/member/profile").with(user(fixture.user().getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(tier))).andReturn();
        var summary = (MemberLoyaltyService.Summary) result.getModelAndView().getModel().get("loyalty");
        amount(total, summary.spending()); assertEquals(tier, summary.tier());
        amount(total, loyalty.summary(fixture.user().getEmail()).spending());
        assertEquals(paymentCount, payments.count()); assertEquals(membershipCount, memberships.count());
    }

    @Test void seededCashCountsDespiteOldExclusionButNeverCreatesLongTermUsageOrFinancialEvidence() {
        var fixture = fixture(); fixture.user().setEmail("member04@utefit.test"); users.flush();
        for (String key : List.of("M04A", "M04NEXT")) {
            var payment = historicalCash(fixture, "300000", "DEMO-CASH-" + key, PaymentStatus.PAID, MembershipStatus.ACTIVE);
            exclusions.saveAndFlush(LoyaltyPaymentExclusion.builder().paymentId(payment.getId())
                    .reason("DB_Data_2.sql V712-demo55: legacy exclusion").reviewedAt(LocalDateTime.now()).build());
        }
        var history = loyalty.history(fixture.user()); var summary = history.summary();
        amount("600000", summary.spending()); assertEquals("SILVER", summary.tier());
        assertEquals(0, summary.usedDays()); assertFalse(summary.loyal()); assertTrue(summary.welcomeAvailable());
        assertTrue(history.paidMemberships().isEmpty());
        var quote = billing.quote(fixture.user().getEmail(), request(fixture, "400000", "AUTO"));
        assertTrue(quote.alternatives().stream().anyMatch(offer -> offer.kind().equals("TIER") && offer.percent().compareTo(new BigDecimal("5")) == 0));
        assertFalse(quote.alternatives().stream().anyMatch(offer -> offer.kind().equals("LOYAL")));
        for (var payment : payments.findByUser_IdOrderByCreatedAtDesc(fixture.user().getId())) {
            assertEquals(PaymentMethod.CASH, payment.getMethod()); assertEquals(PaymentStatus.PAID, payment.getStatus());
            assertNull(payment.getGatewayTransactionNo()); assertTrue(exclusions.existsById(payment.getId()));
        }
    }

    @Test void seededCashStillRejectsUnpaidRefundedInvalidAndDuplicateTransactions() {
        var fixture = fixture(); fixture.user().setEmail("member19@utefit.test"); users.flush();
        var payment = historicalCash(fixture, "2800000", "DEMO-CASH-M19", PaymentStatus.PAID, MembershipStatus.ACTIVE);
        assertEquals("GOLD", loyalty.summary(fixture.user().getEmail()).tier());
        for (var status : List.of(PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.CANCEL_REQUESTED, PaymentStatus.REFUNDED)) {
            payment.setStatus(status); payments.flush(); amount("0", loyalty.summary(fixture.user().getEmail()).spending());
            assertEquals("BRONZE", loyalty.summary(fixture.user().getEmail()).tier());
        }
        payment.setStatus(PaymentStatus.PAID); payment.setPaidAt(null); payments.flush();
        amount("0", loyalty.summary(fixture.user().getEmail()).spending());
        payment.setPaidAt(LocalDateTime.now(SepayPaymentService.ZONE).minusDays(1)); payments.flush();
        historicalCash(fixture, "2800000", "DEMO-CASH-M19", PaymentStatus.PAID, MembershipStatus.ACTIVE);
        amount("2800000", loyalty.summary(fixture.user().getEmail()).spending());
    }

    @Test void tierReadWorksWithAutomaticOffersDisabledAndDoesNotRequirePaymentConfirmation() {
        var fixture = fixture(); fixture.user().setEmail("member19@utefit.test"); users.flush();
        var payment = historicalCash(fixture, "2800000", "DEMO-CASH-M19", PaymentStatus.PAID, MembershipStatus.ACTIVE);
        var policy = new com.utefit.config.LoyaltyProperties();
        var reader = new MemberLoyaltyService(payments, memberships, users, exclusions,
                context.getBean(SepayPaymentDetailsRepository.class), policy, Clock.systemUTC(),
                new AutomaticPromotionService(context.getBean(AutomaticPromotionRepository.class), memberships, policy, Clock.systemUTC()));
        var summary = reader.summary(fixture.user().getEmail());
        assertFalse(summary.enabled()); assertEquals("GOLD", summary.tier()); amount("2800000", summary.spending());
        assertEquals(PaymentStatus.PAID, payment.getStatus()); assertEquals(PaymentMethod.CASH, payment.getMethod());
        assertEquals(1, payments.findByUser_IdOrderByCreatedAtDesc(fixture.user().getId()).size());
    }

    @Test void duplicatePaymentsForOneMembershipAreCountedOnce() {
        var fixture = fixture(); var first = paid(fixture, "500000");
        payments.saveAndFlush(Payment.builder().user(fixture.user()).membership(first.getMembership()).amount(first.getAmount()).status(PaymentStatus.PAID).method(PaymentMethod.CASH).paidAt(first.getPaidAt()).transactionCode("duplicate-" + UUID.randomUUID()).build());
        amount("500000", loyalty.summary(fixture.user().getEmail()).spending());
    }

    @Test void sixCalendarMonthsUseUnionOfElapsedPaidDaysAndVerifiedSamePlanRenewal() {
        var fixture = fixture(); var first = paid(fixture, "300000"); var second = paid(fixture, "300000");
        LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        for (var payment : List.of(first, second)) {
            payment.getMembership().setStartDate(today.minusMonths(6)); payment.getMembership().setEndDate(today);
            payment.setPaidAt(today.minusMonths(6).atStartOfDay());
        }
        memberships.flush(); payments.flush();
        var summary = loyalty.summary(fixture.user().getEmail());
        assertTrue(summary.loyal());
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(today.minusMonths(6), today) + 1, summary.usedDays());
        var input = new MembershipPurchaseRequest(); input.setBranchId(fixture.branch().getId()); input.setPlanId(first.getMembership().getPlan().getId());
        var quote = billing.quote(fixture.user().getEmail(), input);
        assertEquals("LOYAL", quote.kind()); amount("30000", quote.discount());
        first.getMembership().setStartDate(today.minusDays(20)); second.getMembership().setStartDate(today.minusDays(20)); memberships.flush();
        assertFalse(loyalty.summary(fixture.user().getEmail()).loyal());
    }

    @Test void futureUsageAndUnpaidOrCancelledPeriodsNeverGrantLongTermBenefit() {
        var fixture = fixture(); var payment = paid(fixture, "100000");
        payment.getMembership().setStartDate(LocalDate.now(SepayPaymentService.ZONE).plusDays(1));
        payment.getMembership().setEndDate(LocalDate.now(SepayPaymentService.ZONE).plusYears(1)); memberships.flush();
        assertEquals(0, loyalty.summary(fixture.user().getEmail()).usedDays());
        assertFalse(loyalty.summary(fixture.user().getEmail()).loyal());
    }

    @Test void valueThresholdsDoNotStackAndExplicitCodeReplacesAutomaticOffer() {
        var fixture = fixture();
        amount("50000", billing.quote(fixture.user().getEmail(), request(fixture, "500000", "AUTO")).discount());
        var input = request(fixture, "1000000", "AUTO");
        var quote = billing.quote(fixture.user().getEmail(), input);
        amount("100000", quote.discount()); amount("900000", quote.finalPrice());
        var promotion = code("5", 2); input.setPromotionCode(promotion.getCode());
        quote = billing.quote(fixture.user().getEmail(), input);
        assertEquals("CODE", quote.kind()); amount("50000", quote.discount()); amount("950000", quote.finalPrice());
    }

    private Promotion code(String percent, int quantity) {
        LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        return promotions.saveAndFlush(Promotion.builder().code("QA" + UUID.randomUUID().toString().replace("-", "")).discountPercent(new BigDecimal(percent)).startDate(today.minusDays(1)).endDate(today.plusDays(1)).quantity(quantity).active(true).build());
    }

    @Test void manualCodeCapsMinimumAndPerUserLimitAreBackendEnforcedAndPendingCancelRestoresOnlyUnpaidUse() {
        var fixture = fixture(); var promotion = code("20", 2);
        promotion.setMaxDiscountAmount(new BigDecimal("40000")); promotion.setMinOrderAmount(new BigDecimal("300000")); promotion.setPerUserLimit(1); promotions.flush();
        var small = request(fixture, "200000", "AUTO"); small.setPromotionCode(promotion.getCode());
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(fixture.user().getEmail(), small));
        var input = request(fixture, "400000", "AUTO"); input.setPromotionCode(promotion.getCode());
        Long order = billing.createMembership(fixture.user().getEmail(), input);
        amount("360000", memberships.findById(order).orElseThrow().getQuotedPrice());
        var another = request(fixture, "500000", "AUTO"); another.setPromotionCode(promotion.getCode());
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(fixture.user().getEmail(), another));
        assertTrue(billing.cancelPendingMembership(fixture.user().getEmail(), order));
        assertEquals(2, promotion.getQuantity());
        Long paidOrder = billing.createMembership(fixture.user().getEmail(), another);
        billing.confirmCashPayment(billing.requestCashPayment(fixture.user().getEmail(), paidOrder));
        assertEquals(1, memberships.promotionUses(fixture.user().getId(), promotion.getCode()));
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(fixture.user().getEmail(), input));
    }

    @Test void expiredExhaustedAndFreeCodesAreRejected() {
        var fixture = fixture(); var promotion = code("10", 0); var input = request(fixture, "100000", "AUTO"); input.setPromotionCode(promotion.getCode());
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(fixture.user().getEmail(), input));
        promotion.setQuantity(2); promotion.setEndDate(LocalDate.now(SepayPaymentService.ZONE).minusDays(1)); promotions.flush();
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(fixture.user().getEmail(), input));
        promotion.setEndDate(LocalDate.now(SepayPaymentService.ZONE).plusDays(1)); promotion.setDiscountPercent(new BigDecimal("100")); promotions.flush();
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(fixture.user().getEmail(), input));
    }

    @Test void publicCatalogIsPrivateByDefaultProfileRendersTierAndForgedBrowserPriceIsIgnored() throws Exception {
        var fixture = fixture(); var input = request(fixture, "200000", "AUTO");
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/promotions")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Đặc quyền Gold"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(fixture.user().getEmail()))));
        mvc.perform(get("/member/profile").with(user(fixture.user().getEmail()).roles("MEMBER"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("BRONZE"))).andExpect(content().string(org.hamcrest.Matchers.containsString("profile-avatar-dialog")));
        mvc.perform(post("/member/memberships").with(user(fixture.user().getEmail()).roles("MEMBER")).with(csrf()).param("branchId", input.getBranchId().toString()).param("planId", input.getPlanId().toString()).param("amount", "1").param("quotedPrice", "1").param("tier", "GOLD")).andExpect(status().is3xxRedirection());
        var order = memberships.findByUser_IdOrderByCreatedAtDesc(fixture.user().getId()).get(0);
        amount("180000", order.getQuotedPrice()); amount("0", loyalty.summary(fixture.user().getEmail()).spending());
        mvc.perform(get("/member/memberships/quote").with(user(fixture.user().getEmail()).roles("MEMBER")).param("branchId", input.getBranchId().toString()).param("planId", input.getPlanId().toString()).param("offerSelection", "BAD")).andExpect(status().isBadRequest());
    }
}
