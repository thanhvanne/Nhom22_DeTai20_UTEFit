package com.utefit.service;

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
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class FavoriteIntegrationTest {
    @Autowired FavoriteService service;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired TrainerRepository trainers;
    @Autowired FitnessClassRepository classes;
    @Autowired ClassCategoryRepository categories;
    @Autowired ClassRegistrationRepository bookings;
    @Autowired TrainerRequestRepository requests;
    @Autowired WebApplicationContext context;
    private User member;
    private Branch branch;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        member = account(UserRole.MEMBER);
        branch = branches.save(Branch.builder().name("Favorite " + UUID.randomUUID()).address("QA").active(true).build());
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private User account(UserRole role) {
        return users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("QA").lastName("Favorite")
                .password("unused-test-hash").role(role).status(UserStatus.ACTIVE).enabled(true).build());
    }

    @Test
    void allTargetTypesPersistWithoutBookingsOrTrainerRequests() {
        long bookingCount = bookings.count();
        long requestCount = requests.count();
        Trainer trainer = trainers.save(Trainer.builder().user(account(UserRole.PT)).branch(branch).active(true).build());
        ClassCategory category = categories.save(ClassCategory.builder().name("Favorite " + UUID.randomUUID()).active(true).build());
        FitnessClass fitnessClass = classes.save(FitnessClass.builder().name("QA Favorite Class").branch(branch)
                .category(category).trainer(trainer).capacity(5).status(ClassStatus.OPEN).build());
        service.add(member.getEmail(), FavoriteType.BRANCH, branch.getId());
        service.add(member.getEmail(), FavoriteType.TRAINER, trainer.getId());
        service.add(member.getEmail(), FavoriteType.CLASS, fitnessClass.getId());
        assertEquals(3, service.list(member.getEmail()).size());
        assertEquals(3, service.saved(member.getEmail()).size());
        assertEquals(bookingCount, bookings.count());
        assertEquals(requestCount, requests.count());
        assertEquals(5, classes.findById(fitnessClass.getId()).orElseThrow().getCapacity());
    }

    @Test
    void duplicateAndConcurrentSavesAreIdempotent() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Long> save = () -> { start.await(); return service.add(member.getEmail(), FavoriteType.BRANCH, branch.getId()); };
            Future<Long> first = executor.submit(save);
            Future<Long> second = executor.submit(save);
            start.countDown();
            assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, service.list(member.getEmail()).size());
    }

    @Test
    void ownershipAndRemovalDoNotDeleteTarget() {
        Long favoriteId = service.add(member.getEmail(), FavoriteType.BRANCH, branch.getId());
        User other = account(UserRole.MEMBER);
        assertTrue(service.list(other.getEmail()).isEmpty());
        assertThrows(AccessDeniedException.class, () -> service.remove(other.getEmail(), favoriteId));
        service.remove(member.getEmail(), favoriteId);
        service.remove(member.getEmail(), favoriteId);
        assertTrue(service.list(member.getEmail()).isEmpty());
        assertTrue(branches.existsById(branch.getId()));
    }

    @Test
    void disabledOrDeletedTargetsAreDisplayedWithoutPublicLink() {
        service.add(member.getEmail(), FavoriteType.BRANCH, branch.getId());
        branch.setActive(false);
        branches.save(branch);
        assertFalse(service.list(member.getEmail()).get(0).available());
        assertThrows(IllegalArgumentException.class, () -> service.add(member.getEmail(), FavoriteType.BRANCH, branch.getId()));
        branches.deleteById(branch.getId());
        assertFalse(service.list(member.getEmail()).get(0).available());
        assertNull(service.list(member.getEmail()).get(0).url());
    }

    @Test
    void inactiveAndNonMemberCannotSave() {
        User trainer = account(UserRole.PT);
        assertThrows(AccessDeniedException.class, () -> service.add(trainer.getEmail(), FavoriteType.BRANCH, branch.getId()));
        member.setEnabled(false);
        users.save(member);
        assertThrows(AccessDeniedException.class, () -> service.add(member.getEmail(), FavoriteType.BRANCH, branch.getId()));
    }

    @Test
    void endpointsEnforceRoleCsrfOwnershipAndSafeRedirect() throws Exception {
        String action = "/member/favorites/BRANCH/" + branch.getId();
        mvc.perform(post(action).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(action).with(user("pt").roles("PT")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(action).with(user(member.getEmail()).roles("MEMBER"))).andExpect(status().isForbidden());
        mvc.perform(post(action).with(user(member.getEmail()).roles("MEMBER")).with(csrf())
                .param("returnTo", "https://evil.invalid")).andExpect(redirectedUrl("/member/favorites"));
        Long favoriteId = service.list(member.getEmail()).get(0).id();
        User other = account(UserRole.MEMBER);
        mvc.perform(post("/member/favorites/" + favoriteId + "/remove").with(user(other.getEmail()).roles("MEMBER"))
                .with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/branches").with(user(member.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Bỏ yêu thích")));
        mvc.perform(get("/branches")).andExpect(status().isOk());
    }
}
