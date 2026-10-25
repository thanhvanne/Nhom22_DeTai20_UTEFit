package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.MultipartConfigElement;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class BranchPublicReviewIntegrationTest {
    @Autowired BranchRepository branches;
    @Autowired UserRepository users;
    @Autowired ReviewRepository reviews;
    @Autowired PublicReviewService service;
    @Autowired AdminReviewService admin;
    @Autowired WebApplicationContext context;
    @Autowired EntityManagerFactory factory;
    @Autowired MultipartConfigElement multipart;
    private Branch branch;
    private Branch other;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        branch = branches.save(Branch.builder().name("Public " + UUID.randomUUID()).address("Test").active(true).build());
        other = branches.save(Branch.builder().name("Other " + UUID.randomUUID()).address("Test").active(true).build());
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Review review(Branch location, int rating, boolean visible, int minute) {
        User author = users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("Alias")
                .lastName("PrivateSurname").password("unused-test-hash").role(UserRole.MEMBER)
                .status(UserStatus.ACTIVE).enabled(true).build());
        return reviews.saveAndFlush(Review.builder().user(author).branch(location).rating(rating).visible(visible)
                .comment("review-" + UUID.randomUUID()).createdAt(LocalDateTime.of(2026, 10, 26, 0, minute))
                .imageUrl("https://res.cloudinary.com/test/image/upload/old.png")
                .videoUrl("https://res.cloudinary.com/test/video/upload/legacy.mp4").build());
    }

    @Test
    void onlyVisibleReviewsOfExactBranchContributeToAverageAndTotal() {
        Review first = review(branch, 5, true, 1);
        Review second = review(branch, 4, true, 2);
        Review hidden = review(branch, 1, false, 3);
        review(other, 1, true, 4);
        var result = service.forBranch(branch.getId());
        assertEquals(2, result.totalReviews());
        assertEquals(4.5, result.averageRating(), 0.0001);
        assertEquals(java.util.List.of(second.getId(), first.getId()), result.latest().stream().map(PublicReviewService.PublicReview::id).toList());
        assertTrue(result.latest().stream().noneMatch(item -> item.id().equals(hidden.getId())));
        assertEquals("Alias", result.latest().get(0).author());
    }

    @Test
    void latestIsBoundedToFiveWhileSummaryCoversAllVisibleReviewsWithoutNPlusOne() {
        for (int index = 0; index < 8; index++) review(branch, index < 3 ? 1 : 5, true, index);
        var statistics = factory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = service.forBranch(branch.getId());
        assertEquals(8, result.totalReviews());
        assertEquals(3.5, result.averageRating(), 0.0001);
        assertEquals(5, result.latest().size());
        assertTrue(result.latest().stream().allMatch(item -> item.rating() == 5));
        assertEquals(2, statistics.getPrepareStatementCount());
    }

    @Test
    void guestAndAnotherMemberCanSeeLegacyMediaButNotPrivateAuthorFieldsOrOtherBranch() throws Exception {
        Review publicReview = review(branch, 5, true, 1);
        Review unrelated = review(other, 4, true, 2);
        mvc.perform(get("/branches/{id}", branch.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Đánh giá hội viên")))
                .andExpect(content().string(containsString(publicReview.getVideoUrl())))
                .andExpect(content().string(containsString(publicReview.getImageUrl())))
                .andExpect(content().string(containsString("/reviews?branchId=" + branch.getId())))
                .andExpect(content().string(not(containsString("PrivateSurname"))))
                .andExpect(content().string(not(containsString(unrelated.getComment()))));
        User viewer = users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("Viewer").lastName("")
                .password("unused-test-hash").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
        mvc.perform(get("/branches/{id}", branch.getId()).with(user(viewer.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString(publicReview.getComment())));
    }

    @Test
    void adminHideRemovesVideoAndRecalculatesBranchAndExistingPublicList() throws Exception {
        Review removed = review(branch, 5, true, 1);
        review(branch, 3, true, 2);
        admin.toggleVisibility(removed.getId());
        var result = service.forBranch(branch.getId());
        assertEquals(1, result.totalReviews());
        assertEquals(3.0, result.averageRating());
        assertTrue(service.search(branch.getId(), null, 0).stream().noneMatch(item -> item.id().equals(removed.getId())));
        mvc.perform(get("/branches/{id}", branch.getId())).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(removed.getComment()))));
        mvc.perform(get("/reviews").param("branchId", branch.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString(removed.getComment()))));
    }

    @Test
    void branchWithNoVisibleReviewsShowsEmptyStateAndNoFakeZeroRating() throws Exception {
        review(branch, 1, false, 1);
        var result = service.forBranch(branch.getId());
        assertEquals(0, result.totalReviews());
        assertNull(result.averageRating());
        assertTrue(result.latest().isEmpty());
        mvc.perform(get("/branches/{id}", branch.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Chưa có đánh giá công khai")));
    }

    @Test
    void multipartLimitsPermitOneFiftyMiBVideoPlusFiveMiBImageAndOverhead() {
        assertEquals(50L * 1024 * 1024, multipart.getMaxFileSize());
        assertEquals(70L * 1024 * 1024, multipart.getMaxRequestSize());
        assertTrue(multipart.getMaxRequestSize() > multipart.getMaxFileSize() + 5L * 1024 * 1024);
    }
}
