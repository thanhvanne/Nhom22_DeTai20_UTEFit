package com.utefit.service;

import com.utefit.config.SepayProperties;
import com.utefit.dto.member.MembershipPurchaseRequest;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(properties = "utefit.loyalty.enabled=true")
class LoyaltySepayIntegrationTest {
    private static final String SECRET = "isolated-loyalty-fixture-hmac-never-for-live-123456";
    private static final AtomicLong IDS = new AtomicLong(9000000);
    @MockitoBean SepayProperties properties;
    @MockitoBean SepayDatabaseGuard guard;
    @Autowired MembershipPaymentService billing;
    @Autowired SepayPaymentService sepay;
    @Autowired MemberLoyaltyService loyalty;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired PaymentRepository payments;
    @Autowired PromotionRepository promotions;
    @Autowired SepayPaymentDetailsRepository details;
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    private record Order(User member, Long membershipId, Long paymentId, String reference) {}
    @BeforeEach void setup() {
        when(properties.testMode()).thenReturn(false); when(properties.mode()).thenReturn("LIVE");
        when(properties.acceptingNewPayments()).thenReturn(true); when(properties.secret()).thenReturn(SECRET);
        when(properties.getBankCode()).thenReturn("QA-BANK"); when(properties.getGateway()).thenReturn("QABANK");
        when(properties.getAccountNumber()).thenReturn("QA0001"); when(properties.getAccountName()).thenReturn("Isolated H2 only");
        when(properties.getPaymentPrefix()).thenReturn("UTE"); when(properties.getPaymentMinutes()).thenReturn(15);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    private Order order(String code) {
        String identity = UUID.randomUUID().toString();
        var member = users.saveAndFlush(User.builder().email(identity + "@h2-only.test").firstName("QA").lastName("Loyalty").password("unused").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        var branch = branches.saveAndFlush(Branch.builder().name(identity).address("H2 only").active(true).build());
        var plan = plans.saveAndFlush(MembershipPlan.builder().name(identity).price(new BigDecimal("600000")).durationDays(30).branchIds(Set.of(branch.getId())).active(true).build());
        var request = new MembershipPurchaseRequest(); request.setBranchId(branch.getId()); request.setPlanId(plan.getId()); request.setPromotionCode(code);
        Long membershipId = billing.createMembership(member.getEmail(), request);
        Long paymentId = sepay.start(member.getEmail(), membershipId);
        return new Order(member, membershipId, paymentId, details.findById(paymentId).orElseThrow().getPaymentReference());
    }
    private String payload(Order order, long id, String amount) {
        return "{\"id\":" + id + ",\"gateway\":\"QABANK\",\"accountNumber\":\"QA0001\",\"code\":\"" + order.reference() + "\",\"content\":\"" + order.reference()
                + "\",\"transferType\":\"in\",\"transferAmount\":" + amount + ",\"referenceCode\":\"LQ" + id + "\",\"transactionDate\":\"" + LocalDateTime.now(SepayPaymentService.ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\"}";
    }
    private int deliver(String body, boolean validSignature) throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = validSignature ? HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8))) : "0".repeat(64);
        return mvc.perform(post(properties.testMode() ? "/payments/sepay/webhook/test" : "/payments/sepay/webhook/live").secure(true).contentType("application/json").content(body).header("X-SePay-Timestamp", timestamp).header("X-SePay-Signature", "sha256=" + signature)).andReturn().getResponse().getStatus();
    }
    @Test void discountedSnapshotDrivesQrAndOnlyAuthenticatedReceiptUpgradesTierOnce() throws Exception {
        var order = order(null);
        assertEquals(0, new BigDecimal("550000").compareTo(payments.findById(order.paymentId()).orElseThrow().getAmount()));
        assertTrue(sepay.checkout(order.member().getEmail(), order.paymentId()).qrUrl().contains("amount=550000"));
        assertEquals("BRONZE", loyalty.summary(order.member().getEmail()).tier());
        String body = payload(order, IDS.incrementAndGet(), "550000");
        assertEquals(401, deliver(body, false));
        assertEquals(PaymentStatus.PENDING, payments.findById(order.paymentId()).orElseThrow().getStatus());
        assertEquals(200, deliver(body, true)); assertEquals(200, deliver(body, true));
        assertEquals(PaymentStatus.PAID, payments.findById(order.paymentId()).orElseThrow().getStatus());
        assertEquals(MembershipStatus.ACTIVE, memberships.findById(order.membershipId()).orElseThrow().getStatus());
        assertEquals("SILVER", loyalty.summary(order.member().getEmail()).tier());
        assertEquals(0, new BigDecimal("550000").compareTo(loyalty.summary(order.member().getEmail()).spending()));
        assertEquals(0, new BigDecimal("550000").compareTo(memberships.findById(order.membershipId()).orElseThrow().getQuotedPrice()));
    }
    @Test void mismatchCannotGrantSpendingAndDuplicateWebhookDoesNotConsumeCodeAgain() throws Exception {
        String code = "LQ" + UUID.randomUUID().toString().replace("-", "");
        var promotion = promotions.saveAndFlush(Promotion.builder().code(code).discountPercent(new BigDecimal("20")).quantity(2).perUserLimit(1).active(true).startDate(LocalDate.now(SepayPaymentService.ZONE).minusDays(1)).endDate(LocalDate.now(SepayPaymentService.ZONE).plusDays(1)).build());
        var mismatch = order(null);
        assertEquals(200, deliver(payload(mismatch, IDS.incrementAndGet(), "600000"), true));
        assertEquals(PaymentStatus.PENDING, payments.findById(mismatch.paymentId()).orElseThrow().getStatus());
        assertEquals(0, loyalty.summary(mismatch.member().getEmail()).spending().signum());
        var order = order(code); String body = payload(order, IDS.incrementAndGet(), "480000");
        assertEquals(1, promotions.findById(promotion.getId()).orElseThrow().getQuantity());
        assertEquals(200, deliver(body, true)); assertEquals(200, deliver(body, true));
        assertEquals(1, promotions.findById(promotion.getId()).orElseThrow().getQuantity());
        assertEquals(1, memberships.promotionUses(order.member().getId(), code));
        assertEquals(0, new BigDecimal("480000").compareTo(loyalty.summary(order.member().getEmail()).spending()));
    }
    @Test void testEnvironmentPaymentsNeverCountAsLiveSpending() throws Exception {
        when(properties.testMode()).thenReturn(true); when(properties.mode()).thenReturn("TEST");
        var order = order(null); assertEquals(200, deliver(payload(order, IDS.incrementAndGet(), "550000"), true));
        assertEquals(PaymentStatus.PAID, payments.findById(order.paymentId()).orElseThrow().getStatus());
        assertEquals(0, loyalty.summary(order.member().getEmail()).spending().signum());
    }

    @Test void databaseManagedOfferRemainsFrozenForQrAndVerifiedWebhookAfterAdminChanges() throws Exception {
        var repository = context.getBean(AutomaticPromotionRepository.class);
        var programs = context.getBean(AutomaticPromotionService.class);
        var previous = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin@h2-only.test", "unused", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            for (var kind : AutomaticPromotionKind.values()) {
                var form = programs.form(kind);
                repository.saveAndFlush(AutomaticPromotion.builder().kind(kind).active(kind == AutomaticPromotionKind.WELCOME)
                        .percent(kind == AutomaticPromotionKind.WELCOME ? new BigDecimal("15") : form.getPercent())
                        .discountCap(kind == AutomaticPromotionKind.WELCOME ? new BigDecimal("120000") : form.getDiscountCap())
                        .minimumMonths(form.getMinimumMonths()).renewalGraceDays(form.getRenewalGraceDays())
                        .valueThreshold(form.getValueThreshold()).valueDiscount(form.getValueDiscount()).premiumThreshold(form.getPremiumThreshold()).premiumDiscount(form.getPremiumDiscount())
                        .updatedAt(LocalDateTime.now()).updatedBy("Isolated H2 fixture").build());
            }
            var order = order(null);
            var form = programs.form(AutomaticPromotionKind.WELCOME); form.setActive(false); form.setPercent(new BigDecimal("20")); programs.save(AutomaticPromotionKind.WELCOME, form);
            assertEquals(0, new BigDecimal("510000").compareTo(payments.findById(order.paymentId()).orElseThrow().getAmount()));
            assertTrue(sepay.checkout(order.member().getEmail(), order.paymentId()).qrUrl().contains("amount=510000"));
            String body = payload(order, IDS.incrementAndGet(), "510000");
            assertEquals(401, deliver(body, false)); assertEquals(PaymentStatus.PENDING, payments.findById(order.paymentId()).orElseThrow().getStatus());
            assertEquals(200, deliver(body, true)); assertEquals(200, deliver(body, true));
            assertEquals(PaymentStatus.PAID, payments.findById(order.paymentId()).orElseThrow().getStatus());
            assertEquals(MembershipStatus.ACTIVE, memberships.findById(order.membershipId()).orElseThrow().getStatus());
            assertEquals(0, new BigDecimal("510000").compareTo(loyalty.summary(order.member().getEmail()).spending()));
            assertEquals("SILVER", loyalty.summary(order.member().getEmail()).tier());
        } finally {
            repository.deleteAllInBatch();
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
