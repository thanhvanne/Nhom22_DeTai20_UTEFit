package com.utefit.service;

import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "utefit.loyalty.enabled=true")
class LoyaltyConcurrencyIntegrationTest {
    @Autowired MembershipPaymentService billing;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired PromotionRepository promotions;
    private record Input(String email, MembershipPurchaseRequest request) {}
    private Input input(User existing) {
        String identity = UUID.randomUUID().toString();
        User member = existing != null ? existing : users.saveAndFlush(User.builder().email(identity + "@concurrency.test").firstName("QA").lastName("Concurrent").password("unused").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        Branch branch = branches.saveAndFlush(Branch.builder().name(identity).address("H2 only").active(true).build());
        MembershipPlan plan = plans.saveAndFlush(MembershipPlan.builder().name(identity).price(new BigDecimal("200000")).durationDays(30).branchIds(Set.of(branch.getId())).active(true).build());
        var request = new MembershipPurchaseRequest(); request.setBranchId(branch.getId()); request.setPlanId(plan.getId());
        return new Input(member.getEmail(), request);
    }
    private List<Boolean> race(Input first, Input second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (Input input : List.of(first, second)) results.add(executor.submit(() -> {
                start.await();
                try { billing.createMembership(input.email(), input.request()); return true; }
                catch (IllegalArgumentException exception) { return false; }
            }));
            start.countDown();
            List<Boolean> completed = new ArrayList<>();
            for (var result : results) completed.add(result.get(30, TimeUnit.SECONDS));
            return completed;
        } finally { executor.shutdownNow(); }
    }
    private Promotion code(int quantity, Integer perUser) {
        LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        return promotions.saveAndFlush(Promotion.builder().code("RACE" + UUID.randomUUID().toString().replace("-", "")).discountPercent(new BigDecimal("10")).quantity(quantity).perUserLimit(perUser).startDate(today.minusDays(1)).endDate(today.plusDays(1)).active(true).build());
    }
    @Test void oneRemainingCodeCannotBeReservedByTwoDifferentMembers() throws Exception {
        var first = input(null); var second = input(null); var promotion = code(1, null);
        first.request().setPromotionCode(promotion.getCode()); second.request().setPromotionCode(promotion.getCode());
        assertEquals(1, race(first, second).stream().filter(Boolean::booleanValue).count());
        assertEquals(0, promotions.findById(promotion.getId()).orElseThrow().getQuantity());
        assertEquals(1, memberships.promotionReservations(promotion.getCode()));
    }
    @Test void perUserLimitIsSerializedAcrossDifferentOrders() throws Exception {
        var first = input(null); var second = input(users.findByEmail(first.email()).orElseThrow()); var promotion = code(5, 1);
        first.request().setPromotionCode(promotion.getCode()); second.request().setPromotionCode(promotion.getCode());
        assertEquals(1, race(first, second).stream().filter(Boolean::booleanValue).count());
        assertEquals(4, promotions.findById(promotion.getId()).orElseThrow().getQuantity());
    }
    @Test void welcomeCannotBeReservedTwiceByConcurrentRequests() throws Exception {
        var first = input(null); var second = input(users.findByEmail(first.email()).orElseThrow());
        assertEquals(1, race(first, second).stream().filter(Boolean::booleanValue).count());
        var orders = memberships.findByUser_IdOrderByCreatedAtDesc(users.findByEmail(first.email()).orElseThrow().getId());
        assertEquals(1, orders.size()); assertEquals("WELCOME", orders.get(0).getOfferKind());
    }
}
