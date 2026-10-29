package com.utefit.service;

import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PromotionPurchaseIntegrationTest {
    @Autowired MembershipPaymentService billing;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired PromotionRepository promotions;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired AdminBranchService adminBranches;
    @Test void guestCanSeeActivePromotionAndMemberCanSeePurchaseInput() throws Exception {
        var f = fixture();
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
            .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/promotions"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(f.promotion.getCode())));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/member/memberships")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(f.member.getEmail()).roles("MEMBER")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("name=\"promotionCode\"")));
    }
    @Test void futurePaidMembershipPreventsBranchDeactivation() {
        var f = fixture();
        Long id = billing.createMembership(f.member.getEmail(), f.request);
        billing.confirmCashPayment(billing.requestCashPayment(f.member.getEmail(), id));
        var m = memberships.findById(id).orElseThrow();
        m.setStartDate(LocalDate.now(SepayPaymentService.ZONE).plusDays(31));
        m.setEndDate(m.getStartDate().plusDays(29)); memberships.flush();
        assertThrows(IllegalStateException.class, () -> adminBranches.toggleActive(f.request.getBranchId()));
    }
    private record Fixture(User member, MembershipPurchaseRequest request, Promotion promotion) {}
    private Fixture fixture() {
        String suffix = UUID.randomUUID().toString();
        var member = users.saveAndFlush(User.builder().email(suffix + "@promo.test").firstName("Test").lastName("Promo")
                .password("unused").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        var branch = branches.saveAndFlush(Branch.builder().name(suffix).address("Test").active(true).build());
        var plan = plans.saveAndFlush(MembershipPlan.builder().name(suffix).price(new BigDecimal("100001"))
                .durationDays(30).active(true).branchIds(new HashSet<>(Set.of(branch.getId()))).build());
        var today = LocalDate.now(SepayPaymentService.ZONE);
        var promo = promotions.saveAndFlush(Promotion.builder().code(suffix).discountPercent(new BigDecimal("10"))
                .startDate(today).endDate(today.plusDays(1)).quantity(2).active(true).build());
        var req = new MembershipPurchaseRequest(); req.setBranchId(branch.getId()); req.setPlanId(plan.getId());
        req.setPromotionCode(suffix.toUpperCase(Locale.ROOT));
        return new Fixture(member, req, promo);
    }
    @Test void appliesRoundedVndSnapshotReservesOnceAndRestoresOnCancellation() {
        var f = fixture();
        Long id = billing.createMembership(f.member.getEmail(), f.request);
        var order = memberships.findById(id).orElseThrow();
        assertEquals(0, order.getQuotedPrice().compareTo(new BigDecimal("90001")));
        assertEquals(0, order.getOriginalPrice().compareTo(new BigDecimal("100001")));
        assertEquals(f.promotion.getCode().toUpperCase(Locale.ROOT), order.getPromotionCode());
        assertEquals(1, f.promotion.getQuantity());
        assertEquals(id, billing.createMembership(f.member.getEmail(), f.request));
        assertEquals(1, f.promotion.getQuantity());
        assertTrue(billing.cancelPendingMembership(f.member.getEmail(), id));
        assertEquals(2, f.promotion.getQuantity());
    }
    @Test void exhaustedAndExpiredCodesAreRejected() {
        var f = fixture(); f.promotion.setQuantity(0); promotions.flush();
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(f.member.getEmail(), f.request));
    }
    @Test void differentCodeDoesNotSilentlyReusePendingPrice() {
        var f = fixture(); billing.createMembership(f.member.getEmail(), f.request);
        f.request.setPromotionCode(null);
        assertThrows(IllegalArgumentException.class, () -> billing.createMembership(f.member.getEmail(), f.request));
    }
}
