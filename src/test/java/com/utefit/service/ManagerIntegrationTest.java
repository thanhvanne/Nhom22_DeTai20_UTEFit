package com.utefit.service;

import com.utefit.dto.admin.ClassScheduleFormDto;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ManagerIntegrationTest {
    @Autowired ManagerAccessService access;
    @Autowired ManagerOperationsService operations;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired CheckinRepository checkins;
    @Autowired ClassCategoryRepository categories;
    @Autowired FitnessClassRepository classes;
    @Autowired ClassScheduleRepository schedules;
    @Autowired MembershipPlanRepository plans;
    @Autowired MembershipRepository memberships;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired WebApplicationContext context;
    private User admin;
    private User manager;
    private User member;
    private Branch branch;
    private Branch other;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        admin = account(UserRole.ADMIN);
        manager = account(UserRole.MANAGER);
        member = account(UserRole.MEMBER);
        branch = branch("Scoped");
        other = branch("Outside");
        access.grant(admin.getEmail(), manager.getId(), branch.getId());
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private User account(UserRole role) {
        return users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("QA").lastName("Manager")
                .password("unused-test-hash").role(role).status(UserStatus.ACTIVE).enabled(true).build());
    }

    private Branch branch(String prefix) {
        return branches.save(Branch.builder().name(prefix + UUID.randomUUID()).address("QA").active(true).build());
    }

    private FitnessClass fitnessClass(Branch location) {
        ClassCategory category = categories.save(ClassCategory.builder().name(UUID.randomUUID().toString()).active(true).build());
        return classes.save(FitnessClass.builder().name("QA scoped class").branch(location).category(category)
                .capacity(5).status(ClassStatus.OPEN).build());
    }

    private ClassSchedule schedule(Branch location) {
        return schedules.save(ClassSchedule.builder().fitnessClass(fitnessClass(location)).scheduleDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).active(true).build());
    }

    @Test
    void assignmentIsIdempotentAndRevocationAppliesToNextRequest() {
        access.grant(admin.getEmail(), manager.getId(), branch.getId());
        assertEquals(1, access.myBranches(manager.getEmail()).size());
        assertEquals(branch.getId(), access.myBranches(manager.getEmail()).get(0).getId());
        assertThrows(AccessDeniedException.class, () -> operations.history(manager.getEmail(), other.getId(), 0));
        access.revoke(admin.getEmail(), manager.getId(), branch.getId());
        assertTrue(access.myBranches(manager.getEmail()).isEmpty());
        assertThrows(AccessDeniedException.class, () -> operations.history(manager.getEmail(), branch.getId(), 0));
    }

    @Test
    void managerCannotGrantScopeOrCreatePrivilegedAccount() {
        assertThrows(AccessDeniedException.class, () -> access.grant(manager.getEmail(), manager.getId(), other.getId()));
        assertThrows(AccessDeniedException.class, () -> access.createManager(manager.getEmail(), "new@utefit.test", "QA", "unused-only-for-test"));
        assertThrows(IllegalArgumentException.class, () -> access.grant(admin.getEmail(), member.getId(), branch.getId()));
        assertThrows(AccessDeniedException.class, () -> access.myBranches(member.getEmail()));
    }

    @Test
    void adminCreatesOnlyNewManagerWithoutConvertingExistingAccounts() {
        String email = UUID.randomUUID() + "@utefit.test";
        String password = "unused-only-for-test";
        Long id = access.createManager(admin.getEmail(), email, "New Manager", password);
        User created = users.findById(id).orElseThrow();
        assertEquals(UserRole.MANAGER, created.getRole());
        assertTrue(encoder.matches(password, created.getPassword()));
        assertThrows(IllegalArgumentException.class, () -> access.createManager(admin.getEmail(), member.getEmail(), "QA", password));
        assertEquals(UserRole.MEMBER, users.findById(member.getId()).orElseThrow().getRole());
    }

    @Test
    void branchHistoryAndCheckoutCannotLeakAcrossBranchIdOrRecordId() {
        Checkin own = checkins.save(Checkin.builder().user(member).branch(branch).build());
        Checkin outside = checkins.save(Checkin.builder().user(member).branch(other).build());
        assertEquals(1, operations.history(manager.getEmail(), branch.getId(), 0).getTotalElements());
        assertThrows(AccessDeniedException.class, () -> operations.checkOut(manager.getEmail(), branch.getId(), outside.getId()));
        assertThrows(AccessDeniedException.class, () -> operations.checkOut(manager.getEmail(), other.getId(), outside.getId()));
        operations.checkOut(manager.getEmail(), branch.getId(), own.getId());
        assertNotNull(checkins.findById(own.getId()).orElseThrow().getCheckOutAt());
        assertNull(checkins.findById(outside.getId()).orElseThrow().getCheckOutAt());
    }

    @Test
    void scheduleIdAndReplacementClassAreBothScoped() {
        ClassSchedule own = schedule(branch);
        ClassSchedule outside = schedule(other);
        assertEquals(1, operations.schedules(manager.getEmail(), branch.getId(), null, 0).getTotalElements());
        assertThrows(AccessDeniedException.class, () -> operations.editForm(manager.getEmail(), branch.getId(), outside.getId()));
        assertThrows(AccessDeniedException.class, () -> operations.toggleSchedule(manager.getEmail(), branch.getId(), outside.getId()));
        ClassScheduleFormDto forged = ClassScheduleFormDto.builder().fitnessClassId(outside.getFitnessClass().getId())
                .scheduleDate(LocalDate.now().plusDays(3)).startTime(LocalTime.NOON).endTime(LocalTime.of(13, 0)).active(true).build();
        assertThrows(AccessDeniedException.class, () -> operations.saveSchedule(manager.getEmail(), branch.getId(), own.getId(), forged));
        assertThrows(AccessDeniedException.class, () -> operations.saveSchedule(manager.getEmail(), branch.getId(), null, forged));
        assertTrue(schedules.findById(outside.getId()).orElseThrow().getActive());
    }

    @Test
    void ownedScheduleUsesExistingCreateUpdateAndCancelServices() {
        FitnessClass fitnessClass = fitnessClass(branch);
        ClassScheduleFormDto form = ClassScheduleFormDto.builder().fitnessClassId(fitnessClass.getId())
                .scheduleDate(LocalDate.now().plusDays(4)).startTime(LocalTime.NOON).endTime(LocalTime.of(13, 0)).active(true).build();
        operations.saveSchedule(manager.getEmail(), branch.getId(), null, form);
        var created = operations.schedules(manager.getEmail(), branch.getId(), null, 0).getContent().get(0);
        form.setRoom("QA scoped room");
        operations.saveSchedule(manager.getEmail(), branch.getId(), created.getId(), form);
        assertEquals("QA scoped room", schedules.findById(created.getId()).orElseThrow().getRoom());
        operations.toggleSchedule(manager.getEmail(), branch.getId(), created.getId());
        assertFalse(schedules.findById(created.getId()).orElseThrow().getActive());
    }

    @Test
    void managerCheckinReusesMembershipValidationAndCannotChooseOtherBranch() {
        MembershipPlan plan = plans.save(MembershipPlan.builder().name(UUID.randomUUID().toString()).price(new BigDecimal("100000"))
                .durationDays(30).active(true).build());
        memberships.save(Membership.builder().user(member).branch(branch).plan(plan).status(MembershipStatus.ACTIVE)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(29)).build());
        assertThrows(AccessDeniedException.class, () -> operations.checkIn(manager.getEmail(), other.getId(), member.getEmail()));
        operations.checkIn(manager.getEmail(), branch.getId(), member.getEmail());
        assertTrue(checkins.existsByUser_IdAndCheckOutAtIsNull(member.getId()));
    }

    @Test
    void webSecurityBlocksAdminMemberPtAndForgedScope() throws Exception {
        mvc.perform(get("/manager")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/manager").with(user(member.getEmail()).roles("MEMBER"))).andExpect(status().isForbidden());
        mvc.perform(get("/manager").with(user("pt").roles("PT"))).andExpect(status().isForbidden());
        mvc.perform(get("/manager").with(user(manager.getEmail()).roles("MANAGER"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(branch.getName())))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(other.getName()))));
        mvc.perform(get("/manager").param("branchId", other.getId().toString()).with(user(manager.getEmail()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        for (String endpoint : new String[]{"/admin/dashboard", "/admin/payments", "/admin/users", "/admin/managers", "/member/favorites", "/pt/dashboard"}) {
            mvc.perform(get(endpoint).with(user(manager.getEmail()).roles("MANAGER"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/managers").with(user(admin.getEmail()).roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/admin/managers/" + manager.getId() + "/branches").param("branchId", other.getId().toString())
                .with(user(manager.getEmail()).roles("MANAGER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/manager/checkins").with(user(manager.getEmail()).roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void lockedManagerLosesServiceAccessEvenWithStaleAuthority() {
        manager.setStatus(UserStatus.LOCKED);
        users.save(manager);
        assertThrows(AccessDeniedException.class, () -> access.myBranches(manager.getEmail()));
    }
}
