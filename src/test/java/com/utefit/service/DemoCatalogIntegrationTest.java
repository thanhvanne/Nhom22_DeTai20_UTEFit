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
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Transactional
class DemoCatalogIntegrationTest {
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired UserRepository users;
    @Autowired MembershipRepository memberships;
    @Autowired PaymentRepository payments;
    @Autowired MembershipPaymentService billing;
    @Autowired WebApplicationContext context;

    @Test void exactSeedCatalogCreatesCorrectCashSnapshotsForAllFiveBranchesAndRendersVietnamese() throws Exception {
        var seed = Files.readString(Path.of("database/DB_Data_2.sql"));
        var pattern = Pattern.compile("\\((\\d+),N'([^']+)',(\\d+),'(DAY|WEEK|MONTH|YEAR)',(\\d+),'(DAY|WEEK|MONTH|YEAR)','(GYM|YOGA|PT|ALL)',(\\d+)\\)");
        var matcher = pattern.matcher(seed);
        var rows = new ArrayList<List<String>>();
        while (matcher.find()) {
            var row = new ArrayList<String>();
            for (int index = 1; index <= 8; index++) row.add(matcher.group(index));
            rows.add(row);
        }
        assertEquals(11, rows.size());
        var expected = List.of(20000,120000,180000,300000,900000,1500000,2800000,5600000,950000,1600000,3200000);
        var mvc = webAppContextSetup(context).apply(springSecurity()).build();
        for (int branchNumber = 1; branchNumber <= 5; branchNumber++) {
            var branch = branches.save(Branch.builder().name("UTEFit - Chi nhánh " + branchNumber).address("Địa chỉ demo").active(true).build());
            for (var row : rows) {
                var term = new com.utefit.util.MembershipTerm(Integer.parseInt(row.get(2)), MembershipDurationUnit.valueOf(row.get(3)),
                        Integer.parseInt(row.get(4)), MembershipDurationUnit.valueOf(row.get(5)));
                var amount = new BigDecimal(row.get(7));
                assertEquals(0, amount.compareTo(BigDecimal.valueOf(expected.get(Integer.parseInt(row.get(0))-1))));
                var plan = plans.save(MembershipPlan.builder().name(row.get(1)).price(amount).active(true)
                        .durationDays(term.daysAt(java.time.LocalDate.now())).durationUnit(term.unit()).durationQuantity(term.quantity())
                        .bonusUnit(term.bonusUnit()).bonusQuantity(term.bonusQuantity()).benefit(MembershipBenefit.valueOf(row.get(6)))
                        .accessScope(MembershipScope.SINGLE_BRANCH).branchIds(new HashSet<>(Set.of(branch.getId()))).build());
                var member = users.save(User.builder().email(UUID.randomUUID()+"@catalog.test").password("unused")
                        .firstName("Hội viên").lastName("Demo").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
                var request = new MembershipPurchaseRequest(); request.setBranchId(branch.getId()); request.setPlanId(plan.getId());
                var membershipId = billing.createMembership(member.getEmail(), request);
                var paymentId = billing.requestCashPayment(member.getEmail(), membershipId);
                assertEquals(0, amount.compareTo(payments.findById(paymentId).orElseThrow().getAmount()));
                assertEquals(MembershipStatus.PENDING, memberships.findById(membershipId).orElseThrow().getStatus());
                plan.setPrice(amount.add(BigDecimal.ONE)); plans.saveAndFlush(plan);
                assertEquals(0, amount.compareTo(payments.findById(paymentId).orElseThrow().getAmount()));
                billing.confirmCashPayment(paymentId);
                var activated = memberships.findById(membershipId).orElseThrow();
                assertEquals(term.endDate(activated.getStartDate()), activated.getEndDate());
                if (row.get(0).equals("1")) {
                    var response = mvc.perform(get("/member/memberships").with(user(member.getEmail()).roles("MEMBER")))
                            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                    assertTrue(response.contains("UTEFit - Chi nhánh " + branchNumber));
                    assertTrue(response.contains("Gói tập 1 ngày"));
                    assertTrue(response.contains("20.000đ"));
                    assertTrue(response.contains("Phòng gym"));
                    assertFalse(response.contains("20000.00 VND"));
                }
            }
        }
    }
}
