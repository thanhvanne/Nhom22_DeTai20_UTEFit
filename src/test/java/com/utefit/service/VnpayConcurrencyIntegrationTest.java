package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"utefit.vnpay.enabled=true", "utefit.vnpay.tmn-code=CONCURRENCYTEST",
        "utefit.vnpay.hash-secret=unit-test-only-not-a-merchant-secret"})
class VnpayConcurrencyIntegrationTest {
    @Autowired VnpayPaymentService service;
    @Autowired VnpayGateway gateway;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired PaymentRepository payments;
    @Autowired NotificationRepository notifications;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void concurrentCallbacksWithStalePersistenceContextsActivateOnlyOnce() throws Exception {
        User member = users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("QA").lastName("IPN")
                .password("unused-test-hash").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        Branch branch = branches.save(Branch.builder().name("IPN " + UUID.randomUUID()).address("QA").active(true).build());
        MembershipPlan plan = plans.save(MembershipPlan.builder().name("IPN " + UUID.randomUUID())
                .durationDays(90).price(new BigDecimal("150000")).active(true).build());
        Membership membership = memberships.save(Membership.builder().user(member).branch(branch).plan(plan)
                .status(MembershipStatus.PENDING).quotedPrice(new BigDecimal("100000")).quotedDurationDays(30).build());
        service.start(member.getEmail(), membership.getId(), "127.0.0.1");
        Payment payment = payments.findByMembership_IdAndStatus(membership.getId(), PaymentStatus.PENDING).get(0);
        LocalDateTime paidAt = LocalDateTime.now().withNano(0);
        Map<String, String> fields = new TreeMap<>(Map.of("vnp_TmnCode", "CONCURRENCYTEST",
                "vnp_TxnRef", payment.getTransactionCode(), "vnp_Amount", "10000000", "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00", "vnp_TransactionNo", "123456", "vnp_PayDate", VnpayGateway.TIMESTAMP.format(paidAt)));
        fields.put("vnp_SecureHash", gateway.signature(fields));
        long beforeNotifications = notifications.count();
        CountDownLatch loaded = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> callback = () -> new TransactionTemplate(transactions).execute(status -> {
                payments.findById(payment.getId()).orElseThrow();
                memberships.findById(membership.getId()).orElseThrow();
                loaded.countDown();
                try {
                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("QA callback barrier timed out");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return service.ipn(fields).get("RspCode");
            });
            Future<String> first = executor.submit(callback);
            Future<String> second = executor.submit(callback);
            assertTrue(loaded.await(30, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(Set.of("00", "02"), Set.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        assertEquals(PaymentStatus.PAID, payments.findById(payment.getId()).orElseThrow().getStatus());
        Membership active = memberships.findById(membership.getId()).orElseThrow();
        assertEquals(MembershipStatus.ACTIVE, active.getStatus());
        assertEquals(paidAt.toLocalDate(), active.getStartDate());
        assertEquals(paidAt.toLocalDate().plusDays(29), active.getEndDate());
        assertEquals(beforeNotifications + 1, notifications.count());
    }
}
