package com.utefit.service;

import com.utefit.dto.admin.MembershipPlanFormDto;
import com.utefit.entity.Branch;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AdminPlanBranchScopeIntegrationTest {
    @Autowired AdminMembershipPlanService service;
    @Autowired BranchRepository branches;
    @Autowired MembershipPlanRepository plans;
    @Autowired WebApplicationContext context;
    private Branch first;
    private Branch second;
    private Branch inactive;
    private MockMvc mvc;

    @BeforeEach void setup() {
        first = branch(true); second = branch(true); inactive = branch(false);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    private Branch branch(boolean active) {
        return branches.saveAndFlush(Branch.builder().name("Scope H2 " + UUID.randomUUID()).address("H2 only").active(active).build());
    }
    private MembershipPlanFormDto form(MembershipScope scope, Set<Long> selected) {
        return MembershipPlanFormDto.builder().name("Scope plan " + UUID.randomUUID()).durationQuantity(3)
                .durationUnit(MembershipDurationUnit.MONTH).bonusQuantity(2).bonusUnit(MembershipDurationUnit.WEEK)
                .price(new BigDecimal("100000")).accessScope(scope).branchIds(new HashSet<>(selected)).build();
    }
    private Long create(MembershipPlanFormDto form) {
        service.createPlan(form);
        return plans.findAll().stream().filter(plan -> plan.getName().equals(form.getName())).findFirst().orElseThrow().getId();
    }

    @Test void onlyOneFixedActiveBranchIsAllowed() {
        for (var scope : List.of(MembershipScope.MULTI_BRANCH, MembershipScope.ALL_BRANCHES))
            assertThrows(IllegalArgumentException.class, () -> create(form(scope, Set.of(first.getId(), second.getId()))));
        assertThrows(IllegalArgumentException.class, () -> create(form(MembershipScope.SINGLE_BRANCH, Set.of())));
        assertThrows(IllegalArgumentException.class, () -> create(form(MembershipScope.SINGLE_BRANCH, Set.of(first.getId(), second.getId()))));
        var selected = form(MembershipScope.SINGLE_BRANCH, Set.of(first.getId()));
        assertEquals(selected.getBranchIds(), service.getPlanFormById(create(selected)).getBranchIds());
    }

    @Test void unavailableUnknownAndInvalidBranchesCannotBeSaved() {
        for (Long invalid : List.of(inactive.getId(), Long.MAX_VALUE, -1L, 0L))
            assertThrows(IllegalArgumentException.class, () -> create(form(MembershipScope.SINGLE_BRANCH, Set.of(invalid))));
    }

    @Test void calendarTermRoundTripsAndInvalidQuantitiesAreRejected() {
        Long id = create(form(MembershipScope.SINGLE_BRANCH, Set.of(first.getId())));
        var saved = service.getPlanFormById(id);
        assertEquals(MembershipDurationUnit.MONTH, saved.getDurationUnit());
        assertEquals(3, saved.getDurationQuantity()); assertEquals(2, saved.getBonusQuantity());
        assertEquals(MembershipDurationUnit.WEEK, saved.getBonusUnit());
        saved.setBonusQuantity(-1);
        assertThrows(IllegalArgumentException.class, () -> service.updatePlan(id, saved));
        saved.setBonusQuantity(0); saved.setDurationUnit(MembershipDurationUnit.YEAR); saved.setDurationQuantity(101);
        assertThrows(IllegalArgumentException.class, () -> service.updatePlan(id, saved));
    }

    @Test void editRendersSingleBranchDropdownAndCalendarControlsWithoutFlexChoices() throws Exception {
        Long id = create(form(MembershipScope.SINGLE_BRANCH, Set.of(first.getId())));
        mvc.perform(get("/admin/membership-plans/{id}/edit", id).with(user("admin@scope.test").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("id=\"planBranch\"")))
                .andExpect(content().string(containsString("name=\"durationUnit\"")))
                .andExpect(content().string(containsString("name=\"bonusQuantity\"")))
                .andExpect(content().string(not(containsString("value=\"MULTI_BRANCH\""))))
                .andExpect(content().string(not(containsString("value=\"ALL_BRANCHES\""))))
                .andExpect(content().string(not(containsString("plan-branch-picker"))));
    }

    @Test void editShowsPreviouslySelectedInactiveBranchButCannotSaveIt() throws Exception {
        Long id = create(form(MembershipScope.SINGLE_BRANCH, Set.of(first.getId())));
        first.setActive(false); branches.saveAndFlush(first);
        mvc.perform(get("/admin/membership-plans/{id}/edit", id).with(user("admin@scope.test").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString(first.getName())));
        assertThrows(IllegalArgumentException.class, () -> service.updatePlan(id, service.getPlanFormById(id)));
    }

    @Test void controllerRejectsForgedMultiBranchPostWithoutWriting() throws Exception {
        long before = plans.count();
        mvc.perform(post("/admin/membership-plans/create").with(user("admin@scope.test").roles("ADMIN")).with(csrf())
                        .param("name", "HTTP scope " + UUID.randomUUID()).param("durationQuantity", "3").param("durationUnit", "MONTH")
                        .param("bonusQuantity", "2").param("bonusUnit", "WEEK").param("price", "100000")
                        .param("benefit", "GYM").param("accessScope", "MULTI_BRANCH").param("branchIds", first.getId().toString(), second.getId().toString()))
                .andExpect(status().isOk()).andExpect(view().name("admin/plans/form"))
                .andExpect(model().attribute("errorMessage", containsString("đúng một")));
        assertEquals(before, plans.count());
    }

    @Test void controllerSavesSingleCalendarPlanAndPreservesRbacAndCsrf() throws Exception {
        mvc.perform(post("/admin/membership-plans/create").with(user("admin@scope.test").roles("ADMIN")).with(csrf())
                        .param("name", "HTTP fixed " + UUID.randomUUID()).param("durationQuantity", "1").param("durationUnit", "YEAR")
                        .param("bonusQuantity", "1").param("bonusUnit", "MONTH").param("price", "100000")
                        .param("benefit", "GYM").param("accessScope", "SINGLE_BRANCH").param("branchIds", first.getId().toString()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/membership-plans/create").with(user("member@scope.test").roles("MEMBER"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/membership-plans/create").with(user("admin@scope.test").roles("ADMIN"))).andExpect(status().isForbidden());
    }
}
