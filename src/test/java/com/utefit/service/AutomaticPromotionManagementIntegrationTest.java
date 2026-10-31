package com.utefit.service;

import com.utefit.config.LoyaltyProperties;
import com.utefit.dto.admin.AutomaticPromotionForm;
import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(properties = "utefit.loyalty.enabled=false")
@Transactional
@WithMockUser(username = "admin@automatic.test", roles = "ADMIN")
class AutomaticPromotionManagementIntegrationTest {
    @Autowired AutomaticPromotionService programs;
    @Autowired AutomaticPromotionRepository repository;
    @Autowired MembershipPaymentService billing;
    @Autowired MemberLoyaltyService loyalty;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired PaymentRepository payments;
    @Autowired WebApplicationContext context;
    @Autowired jakarta.persistence.EntityManager entityManager;
    private MockMvc mvc;
    private record Fixture(User member, Branch branch) {}

    @BeforeEach void initializeOnlyIsolatedH2Configuration() {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (var kind : AutomaticPromotionKind.values()) {
            var row = AutomaticPromotion.builder().kind(kind).active(false).updatedAt(LocalDateTime.now()).updatedBy("H2 fixture").build();
            switch (kind) {
                case WELCOME -> { row.setPercent(new BigDecimal("10")); row.setDiscountCap(new BigDecimal("50000")); }
                case LOYAL -> { row.setPercent(new BigDecimal("10")); row.setMinimumMonths(6); row.setRenewalGraceDays(30); }
                case VALUE -> { row.setValueThreshold(new BigDecimal("500000")); row.setValueDiscount(new BigDecimal("50000")); row.setPremiumThreshold(new BigDecimal("1000000")); row.setPremiumDiscount(new BigDecimal("100000")); }
                case SILVER -> row.setPercent(new BigDecimal("5")); case GOLD -> row.setPercent(new BigDecimal("10"));
            }
            repository.saveAndFlush(row);
        }
    }

    private Fixture fixture() {
        String identity = UUID.randomUUID().toString();
        var member = users.saveAndFlush(User.builder().email(identity + "@automatic.test").firstName("H2").lastName("Member").password("unused").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        var branch = branches.saveAndFlush(Branch.builder().name(identity).address("H2 only").active(true).build());
        return new Fixture(member, branch);
    }
    private MembershipPurchaseRequest input(Fixture fixture, String amount) {
        var plan = plans.saveAndFlush(MembershipPlan.builder().name(UUID.randomUUID().toString()).price(new BigDecimal(amount)).durationDays(30).branchIds(Set.of(fixture.branch().getId())).active(true).build());
        var input = new MembershipPurchaseRequest(); input.setBranchId(fixture.branch().getId()); input.setPlanId(plan.getId());
        return input;
    }
    private Payment paid(Fixture fixture, String amount) {
        var input = input(fixture, amount); input.setOfferSelection("NONE");
        Long order = billing.createMembership(fixture.member().getEmail(), input);
        Long payment = billing.requestCashPayment(fixture.member().getEmail(), order); billing.confirmCashPayment(payment);
        return payments.findById(payment).orElseThrow();
    }
    private void admin() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin@automatic.test", "unused",
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
    private void enable(AutomaticPromotionKind kind) { admin(); programs.setActive(kind, programs.form(kind).getVersion(), true); }
    private void money(String amount, BigDecimal actual) { assertEquals(0, new BigDecimal(amount).compareTo(actual)); }
    private org.springframework.util.MultiValueMap<String, String> params(AutomaticPromotionForm form) {
        var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
        var wrapper = new org.springframework.beans.BeanWrapperImpl(form);
        for (var property : wrapper.getPropertyDescriptors()) {
            if (property.getName().equals("class")) continue;
            var value = wrapper.getPropertyValue(property.getName());
            if (value != null) params.set(property.getName(), value.toString());
        }
        return params;
    }

    @Test void adminCanEnablePauseAndEditWelcomeWithoutEnvironmentFlagOrRestart() throws Exception {
        var fixture = fixture(); var input = input(fixture, "200000");
        assertEquals("NONE", billing.quote(fixture.member().getEmail(), input).kind());
        mvc.perform(post("/admin/promotions/programs/WELCOME/state").with(csrf()).param("version", "0").param("active", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        money("180000", billing.quote(fixture.member().getEmail(), input).finalPrice());
        var form = programs.form(AutomaticPromotionKind.WELCOME); form.setPercent(new BigDecimal("15"));
        mvc.perform(post("/admin/promotions/programs/WELCOME").with(csrf()).params(params(form)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        entityManager.clear(); money("170000", billing.quote(fixture.member().getEmail(), input).finalPrice());
        admin();
        programs.setActive(AutomaticPromotionKind.WELCOME, programs.form(AutomaticPromotionKind.WELCOME).getVersion(), false);
        assertEquals("NONE", billing.quote(fixture.member().getEmail(), input).kind());
    }

    @Test void tierRatesAndCapsAreDynamicButNeverChangeTierThresholdsOrCashDemoSpending() {
        var silver = fixture(); paid(silver, "500000");
        var form = programs.form(AutomaticPromotionKind.SILVER); form.setActive(true); form.setPercent(new BigDecimal("12")); form.setDiscountCap(new BigDecimal("25000")); programs.save(AutomaticPromotionKind.SILVER, form);
        money("375000", billing.quote(silver.member().getEmail(), input(silver, "400000")).finalPrice());
        assertEquals("SILVER", loyalty.summary(silver.member().getEmail()).tier());
        var gold = fixture(); gold.member().setEmail("member19@utefit.test"); users.flush();
        var payment = paid(gold, "1500000"); payment.setTransactionCode("DEMO-CASH-M19"); payments.flush();
        form = programs.form(AutomaticPromotionKind.GOLD); form.setActive(true); form.setPercent(new BigDecimal("20")); form.setDiscountCap(new BigDecimal("30000")); programs.save(AutomaticPromotionKind.GOLD, form);
        var quote = billing.quote(gold.member().getEmail(), input(gold, "400000"));
        assertEquals("TIER", quote.kind()); money("370000", quote.finalPrice());
        money("1500000", loyalty.summary(gold.member().getEmail()).spending()); assertEquals("GOLD", loyalty.summary(gold.member().getEmail()).tier());
        money("500000", context.getBean(LoyaltyProperties.class).getSilverThreshold()); money("1500000", context.getBean(LoyaltyProperties.class).getGoldThreshold());
    }

    @Test void valueUsesChangedOriginalPriceThresholdsWithoutStacking() {
        var form = programs.form(AutomaticPromotionKind.VALUE); form.setActive(true); form.setValueThreshold(new BigDecimal("600000")); form.setValueDiscount(new BigDecimal("60000")); form.setPremiumThreshold(new BigDecimal("1200000")); form.setPremiumDiscount(new BigDecimal("120000")); programs.save(AutomaticPromotionKind.VALUE, form);
        var fixture = fixture();
        money("500000", billing.quote(fixture.member().getEmail(), input(fixture, "500000")).finalPrice());
        money("540000", billing.quote(fixture.member().getEmail(), input(fixture, "600000")).finalPrice());
        money("1080000", billing.quote(fixture.member().getEmail(), input(fixture, "1200000")).finalPrice());
        enable(AutomaticPromotionKind.WELCOME);
        money("120000", billing.quote(fixture.member().getEmail(), input(fixture, "1200000")).discount());
    }

    @Test void loyalMonthsAndVerifiedRenewalGraceAreDynamic() {
        var fixture = fixture(); var payment = paid(fixture, "200000");
        LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        payment.setPaidAt(today.minusMonths(7).atStartOfDay()); payment.getMembership().setStartDate(today.minusMonths(7)); payment.getMembership().setEndDate(today.minusDays(1)); payments.flush(); memberships.flush();
        var input = new MembershipPurchaseRequest(); input.setPlanId(payment.getMembership().getPlan().getId()); input.setBranchId(fixture.branch().getId());
        var form = programs.form(AutomaticPromotionKind.LOYAL); form.setActive(true); form.setMinimumMonths(9); form.setPercent(new BigDecimal("15")); programs.save(AutomaticPromotionKind.LOYAL, form);
        assertEquals("NONE", billing.quote(fixture.member().getEmail(), input).kind());
        form = programs.form(AutomaticPromotionKind.LOYAL); form.setMinimumMonths(6); form.setRenewalGraceDays(0); programs.save(AutomaticPromotionKind.LOYAL, form);
        assertEquals("NONE", billing.quote(fixture.member().getEmail(), input).kind());
        form = programs.form(AutomaticPromotionKind.LOYAL); form.setRenewalGraceDays(1); programs.save(AutomaticPromotionKind.LOYAL, form);
        var quote = billing.quote(fixture.member().getEmail(), input); assertEquals("LOYAL", quote.kind()); money("170000", quote.finalPrice());
    }

    @Test void datesAreInclusiveAndUnavailableProgramsAreNotAppliedOrAdvertisedAsEligible() {
        var fixture = fixture(); var input = input(fixture, "200000"); LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        var form = programs.form(AutomaticPromotionKind.WELCOME); form.setActive(true); form.setStartDate(today.plusDays(1)); programs.save(AutomaticPromotionKind.WELCOME, form);
        assertEquals("NONE", billing.quote(fixture.member().getEmail(), input).kind());
        assertFalse(context.getBean(PromotionCatalogService.class).programs(fixture.member().getEmail()).get(0).eligible());
        form = programs.form(AutomaticPromotionKind.WELCOME); form.setStartDate(today); form.setEndDate(today); programs.save(AutomaticPromotionKind.WELCOME, form);
        assertEquals("WELCOME", billing.quote(fixture.member().getEmail(), input).kind());
        form = programs.form(AutomaticPromotionKind.WELCOME); form.setStartDate(null); form.setEndDate(today.minusDays(1)); programs.save(AutomaticPromotionKind.WELCOME, form);
        assertEquals("NONE", billing.quote(fixture.member().getEmail(), input).kind());
    }

    @Test void editingOrPausingDoesNotRepricePendingSnapshotOrReleaseWelcomeReservation() {
        enable(AutomaticPromotionKind.WELCOME); var fixture = fixture(); var input = input(fixture, "200000");
        Long order = billing.createMembership(fixture.member().getEmail(), input);
        var form = programs.form(AutomaticPromotionKind.WELCOME); form.setPercent(new BigDecimal("15")); programs.save(AutomaticPromotionKind.WELCOME, form);
        programs.setActive(AutomaticPromotionKind.WELCOME, programs.form(AutomaticPromotionKind.WELCOME).getVersion(), false);
        money("180000", billing.quote(fixture.member().getEmail(), input).finalPrice());
        assertEquals(order, billing.createMembership(fixture.member().getEmail(), input));
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(fixture.member().getEmail(), input(fixture, "300000")));
        assertEquals(1, programs.usage(AutomaticPromotionKind.WELCOME).pending());
        billing.confirmCashPayment(billing.requestCashPayment(fixture.member().getEmail(), order));
        money("180000", loyalty.summary(fixture.member().getEmail()).spending());
        assertEquals(1, programs.usage(AutomaticPromotionKind.WELCOME).paid()); assertEquals(0, programs.usage(AutomaticPromotionKind.WELCOME).pending());
    }

    @Test void invalidFieldsAndArbitraryKindsAreRejectedWithoutSavingOrEnabling() throws Exception {
        for (String percent : List.of("0", "-5", "100", "15.123")) {
            var form = programs.form(AutomaticPromotionKind.WELCOME); form.setActive(true); form.setPercent(new BigDecimal(percent));
            mvc.perform(post("/admin/promotions/programs/WELCOME").with(csrf()).params(params(form))).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("programForm", "percent"));
            assertFalse(repository.findById(AutomaticPromotionKind.WELCOME).orElseThrow().isActive());
        }
        var form = programs.form(AutomaticPromotionKind.VALUE); form.setPremiumThreshold(form.getValueThreshold());
        var conflictingValue = form;
        admin();
        assertThrows(IllegalArgumentException.class, () -> programs.save(AutomaticPromotionKind.VALUE, conflictingValue));
        form = programs.form(AutomaticPromotionKind.WELCOME); form.setDiscountCap(BigDecimal.ZERO); assertTrue(programs.errors(AutomaticPromotionKind.WELCOME, form).containsKey("discountCap"));
        form = programs.form(AutomaticPromotionKind.VALUE); form.setValueDiscount(form.getValueThreshold()); assertTrue(programs.errors(AutomaticPromotionKind.VALUE, form).containsKey("valueDiscount"));
        form = programs.form(AutomaticPromotionKind.LOYAL); form.setMinimumMonths(0); form.setRenewalGraceDays(-1); assertEquals(2, programs.errors(AutomaticPromotionKind.LOYAL, form).size());
        mvc.perform(post("/admin/promotions/programs/ARBITRARY").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/promotions/programs/WELCOME").with(csrf()).param("version", "0").param("active", "true").param("percent", "bad"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("programForm"));
    }

    @Test void csrfAndAdminAuthorizationRemainRequiredAndStaleSavesCannotOverwrite() throws Exception {
        var form = programs.form(AutomaticPromotionKind.WELCOME);
        mvc.perform(post("/admin/promotions/programs/WELCOME").params(params(form))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/promotions/programs/WELCOME/state").with(user("member@automatic.test").roles("MEMBER")).with(csrf()).param("version", "0").param("active", "true")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/promotions/programs/WELCOME").with(user("manager@automatic.test").roles("MANAGER"))).andExpect(status().isForbidden());
        enable(AutomaticPromotionKind.WELCOME);
        mvc.perform(post("/admin/promotions/programs/WELCOME").with(csrf()).params(params(form)))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("programForm"));
        assertTrue(repository.findById(AutomaticPromotionKind.WELCOME).orElseThrow().isActive());
    }

    @Test void publicCatalogAndAdminPagesRenderDynamicValuesAndExistingCodeCrudStillWorks() throws Exception {
        enable(AutomaticPromotionKind.WELCOME);
        mvc.perform(get("/admin/promotions")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/promotions/programs/WELCOME/state"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Cấu hình tại utefit.loyalty.*"))));
        for (var kind : AutomaticPromotionKind.values()) mvc.perform(get("/admin/promotions/programs/" + kind)).andExpect(status().isOk()).andExpect(model().attributeExists("programForm"));
        mvc.perform(get("/promotions").with(anonymous())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng nhập để kiểm tra")));
        mvc.perform(post("/admin/promotions/save").with(csrf()).param("code", "AUTOQA").param("discountPercent", "5").param("quantity", "3").param("active", "true").param("startDate", LocalDate.now().minusDays(1).toString()).param("endDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        var fixture = fixture(); var input = input(fixture, "200000"); input.setPromotionCode("AUTOQA");
        var quote = billing.quote(fixture.member().getEmail(), input); assertEquals("CODE", quote.kind()); money("190000", quote.finalPrice());
    }
}
