package com.utefit.service;

import com.utefit.dto.cloudinary.MediaUploadResult;
import com.utefit.dto.member.ReviewForm;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReviewVideoIntegrationTest {
    @Autowired MemberReviewService service;
    @Autowired PublicReviewService publicReviews;
    @Autowired AdminReviewService admin;
    @Autowired UserRepository users;
    @Autowired BranchRepository branches;
    @Autowired CheckinRepository checkins;
    @Autowired ReviewRepository reviews;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @MockitoBean CloudinaryService cloudinary;
    private User member;
    private Branch branch;
    private ReviewForm form;
    private final MockMultipartFile video = new MockMultipartFile("video", "qa.mp4", "video/mp4", new byte[]{1, 2, 3});

    @BeforeEach
    void setup() {
        member = account();
        branch = branches.save(Branch.builder().name("Video " + UUID.randomUUID()).address("QA").active(true).build());
        checkins.save(Checkin.builder().user(member).branch(branch).checkInAt(LocalDateTime.now().minusHours(1))
                .checkOutAt(LocalDateTime.now()).build());
        form = new ReviewForm();
        form.setBranchId(branch.getId());
        form.setRating(5);
        form.setComment("QA video review");
        when(cloudinary.uploadVideo(any(), anyString())).thenReturn(new MediaUploadResult("https://res.cloudinary.com/qa/video/upload/new.mp4", "new-video"));
    }

    private User account() {
        return users.save(User.builder().email(UUID.randomUUID() + "@utefit.test").firstName("QA").lastName("Video")
                .password("unused-test-hash").role(UserRole.MEMBER).status(UserStatus.ACTIVE).enabled(true).build());
    }

    @Test
    void uploadPreservesImageAndExposesVideoThroughExistingPublicReview() {
        var image = new MockMultipartFile("image", "qa.png", "image/png", new byte[]{1});
        when(cloudinary.uploadImage(any(), anyString())).thenReturn(new MediaUploadResult("https://res.cloudinary.com/qa/image/upload/photo.png", "photo"));
        Long id = service.create(member.getEmail(), form, image, video);
        Review review = reviews.findById(id).orElseThrow();
        assertEquals("photo", review.getImagePublicId());
        assertEquals("new-video", review.getVideoPublicId());
        assertNotNull(publicReviews.search(branch.getId(), null, 0).getContent().get(0).videoUrl());
        assertNotNull(service.editForm(member.getEmail(), id).videoUrl());
        verify(cloudinary, never()).deleteVideo("new-video");
    }

    @Test
    void oversizedRequestHasVietnameseHtmlError() throws Exception {
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/error")
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 413)
                        .accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(413))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.containsString("Video đánh giá tối đa 50 MiB")));
    }

    @Test
    void rollbackDeletesNewVideoWithoutPersistingReview() {
        long before = reviews.count();
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.create(member.getEmail(), form, null, video);
            status.setRollbackOnly();
        });
        assertEquals(before, reviews.count());
        verify(cloudinary).deleteVideo("new-video");
    }

    @Test
    void eligibilityAndRatingAreCheckedBeforeUpload() {
        User inexperienced = account();
        assertThrows(IllegalArgumentException.class, () -> service.create(inexperienced.getEmail(), form, null, video));
        form.setRating(6);
        assertThrows(IllegalArgumentException.class, () -> service.create(member.getEmail(), form, null, video));
        verify(cloudinary, never()).uploadVideo(any(), anyString());
    }

    @Test
    void failedUploadRollsBackAndKeepsPreviousVideoAndImage() {
        var image = new MockMultipartFile("image", "old.png", "image/png", new byte[]{1});
        when(cloudinary.uploadImage(any(), anyString())).thenReturn(new MediaUploadResult("https://res.cloudinary.com/test/image/upload/old.png", "old-photo"));
        Long id = service.create(member.getEmail(), form, image, video);
        when(cloudinary.uploadImage(any(), anyString())).thenReturn(new MediaUploadResult("https://res.cloudinary.com/test/image/upload/new.png", "new-photo"));
        when(cloudinary.uploadVideo(any(), anyString())).thenThrow(new IllegalStateException("QA provider unavailable"));
        form.setComment("should rollback");
        assertThrows(IllegalStateException.class, () -> service.update(member.getEmail(), id, form, image, video));
        Review review = reviews.findById(id).orElseThrow();
        assertEquals("QA video review", review.getComment());
        assertEquals("new-video", review.getVideoPublicId());
        assertEquals("old-photo", review.getImagePublicId());
        verify(cloudinary).delete("new-photo");
        verify(cloudinary, never()).delete("old-photo");
        verify(cloudinary, never()).deleteVideo(anyString());
    }

    @Test
    void replacingOrRemovingVideoDeletesOldAssetOnlyAfterCommit() {
        Long id = service.create(member.getEmail(), form, null, video);
        when(cloudinary.uploadVideo(any(), anyString())).thenReturn(new MediaUploadResult("https://res.cloudinary.com/qa/video/upload/replaced.mp4", "replacement"));
        service.update(member.getEmail(), id, form, null, video);
        verify(cloudinary).deleteVideo("new-video");
        assertEquals("replacement", reviews.findById(id).orElseThrow().getVideoPublicId());
        form.setRemoveVideo(true);
        service.update(member.getEmail(), id, form, null, null);
        verify(cloudinary).deleteVideo("replacement");
        assertNull(reviews.findById(id).orElseThrow().getVideoUrl());
    }

    @Test
    void hiddenReviewRemainsHiddenAfterEditAndVideoDoesNotAppearPublicly() {
        Long id = service.create(member.getEmail(), form, null, video);
        admin.toggleVisibility(id);
        service.update(member.getEmail(), id, form, null, null);
        assertFalse(reviews.findById(id).orElseThrow().getVisible());
        assertTrue(publicReviews.search(branch.getId(), null, 0).isEmpty());
        assertTrue(publicReviews.featured().stream().noneMatch(review -> review.id().equals(id)));
    }

    @Test
    void anotherMemberCannotEditOrUploadAndDuplicateDoesNotUpload() {
        Long id = service.create(member.getEmail(), form, null, video);
        clearInvocations(cloudinary);
        User other = account();
        assertThrows(ResponseStatusException.class, () -> service.update(other.getEmail(), id, form, null, video));
        assertThrows(IllegalArgumentException.class, () -> service.create(member.getEmail(), form, null, video));
        verify(cloudinary, never()).uploadVideo(any(), anyString());
    }

    @Test
    void legacyReviewWithoutMediaStillWorksAndConflictingRemovalRollsBack() {
        Long id = service.create(member.getEmail(), form, null);
        assertNull(service.editForm(member.getEmail(), id).videoUrl());
        form.setRemoveVideo(true);
        assertThrows(IllegalArgumentException.class, () -> service.update(member.getEmail(), id, form, null, video));
        assertNull(reviews.findById(id).orElseThrow().getVideoPublicId());
        verify(cloudinary, never()).uploadVideo(any(), anyString());
    }

    @Test
    void failedVideoUploadRollsBackNewReviewAndCleansNewImage() {
        long before = reviews.count();
        var image = new MockMultipartFile("image", "test.png", "image/png", new byte[]{1});
        when(cloudinary.uploadImage(any(), anyString())).thenReturn(new MediaUploadResult("https://res.cloudinary.com/test/image/upload/new.png", "new-photo"));
        when(cloudinary.uploadVideo(any(), anyString())).thenThrow(new IllegalStateException("unit-only provider failure"));
        assertThrows(IllegalStateException.class, () -> service.create(member.getEmail(), form, image, video));
        assertEquals(before, reviews.count());
        verify(cloudinary).delete("new-photo");
    }
}
