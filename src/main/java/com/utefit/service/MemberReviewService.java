package com.utefit.service;

import com.utefit.dto.cloudinary.MediaUploadResult;
import com.utefit.dto.member.ReviewForm;
import com.utefit.entity.*;
import com.utefit.enums.RegistrationStatus;
import com.utefit.enums.PersonalTrainingStatus;
import com.utefit.repository.BranchRepository;
import com.utefit.repository.ReviewRepository;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MemberReviewService {

    private static final Logger log =
            LoggerFactory.getLogger(MemberReviewService.class);

    private final UserRepository users;
    private final ReviewRepository reviews;
    private final BranchRepository branches;
    private final TrainerRepository trainers;
    private final CloudinaryService cloudinary;
    private final MemberAccountService accounts;

    public record BranchOption(Long id, String name) {}

    public record TrainerOption(
            Long id, String name, Long branchId
    ) {}

    public record ReviewTargets(
            List<BranchOption> branches,
            List<TrainerOption> trainers
    ) {}

    public record ReviewView(
            Long id,
            String branchName,
            String trainerName,
            Integer rating,
            String comment,
            String imageUrl,
            String videoUrl,
            boolean visible,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record EditView(
            Long id, ReviewForm form, String imageUrl, String videoUrl
    ) {}

    @Transactional(readOnly = true)
    public ReviewTargets eligibleTargets(String email) {
        return evidence(
                accounts.requireMemberId(email)
        ).targets();
    }

    @Transactional(readOnly = true)
    public Page<ReviewView> myReviews(String email, int page) {
        return reviews.findByUser_IdOrderByCreatedAtDescIdDesc(
                accounts.requireMemberId(email),
                PageRequest.of(Math.max(0, page), 10)
        ).map(this::toView);
    }

    @Transactional(readOnly = true)
    public EditView editForm(String email, Long id) {
        Review r = ownReview(
                accounts.requireMemberId(email), id
        );

        ReviewForm form = new ReviewForm();
        form.setBranchId(r.getBranch().getId());
        form.setTrainerId(
                r.getTrainer() == null
                        ? null : r.getTrainer().getId()
        );
        form.setRating(r.getRating());
        form.setComment(r.getComment());

        return new EditView(
                r.getId(), form, r.getImageUrl(), r.getVideoUrl()
        );
    }

    @Transactional
    public Long create(
            String email,
            ReviewForm form,
            MultipartFile image
    ) {
        return create(email, form, image, null);
    }

    @Transactional
    public Long create(String email, ReviewForm form, MultipartFile image, MultipartFile video) {
        // Khóa Member để hai POST đồng thời không tạo Review trùng.
        User user = lockedMember(email);

        validateRating(form);

        Evidence evidence = evidence(user.getId());

        checkEvidence(
                evidence,
                form.getBranchId(),
                form.getTrainerId()
        );

        if (form.getTrainerId() == null) {

            if (reviews.existsByUser_IdAndBranch_IdAndTrainerIsNull(
                    user.getId(), form.getBranchId()
            )) {
                throw new IllegalArgumentException(
                        "Bạn đã đánh giá chi nhánh này; "
                                + "hãy chỉnh sửa đánh giá cũ."
                );
            }

        } else if (reviews.existsByUser_IdAndTrainer_Id(
                user.getId(), form.getTrainerId()
        )) {
            throw new IllegalArgumentException(
                    "Bạn đã đánh giá PT này; "
                            + "hãy chỉnh sửa đánh giá cũ."
            );
        }

        Branch branch = branches.findById(
                form.getBranchId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Không tìm thấy chi nhánh."
                )
        );

        Trainer trainer = form.getTrainerId() == null
                ? null
                : trainers.findById(form.getTrainerId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy PT."
                        )
                );

        Review review = Review.builder()
                .user(user)
                .branch(branch)
                .trainer(trainer)
                .rating(form.getRating())
                .comment(clean(form.getComment()))
                .visible(true)
                .build();

        MediaUploadResult uploaded = upload(image, null);

        if (uploaded != null) {
            review.setImageUrl(uploaded.url());
            review.setImagePublicId(uploaded.publicId());
        }

        replaceVideo(review, video, false);
        return reviews.saveAndFlush(review).getId();
    }

    @Transactional
    public void update(
            String email,
            Long id,
            ReviewForm form,
            MultipartFile image
    ) {
        update(email, id, form, image, null);
    }

    @Transactional
    public void update(String email, Long id, ReviewForm form, MultipartFile image, MultipartFile video) {
        User user = lockedMember(email);

        validateRating(form);

        Review review = ownReview(user.getId(), id);

        if (video != null && !video.isEmpty()) {
            checkEvidence(evidence(user.getId()), review.getBranch().getId(),
                    review.getTrainer() == null ? null : review.getTrainer().getId());
        }

        // Đối tượng Review là bất biến.
        // Không nhận branchId/trainerId mới từ form.
        review.setRating(form.getRating());
        review.setComment(clean(form.getComment()));

        // KHÔNG gán visible = true.
        // Review đã bị Admin ẩn phải tiếp tục bị ẩn.

        MediaUploadResult uploaded = upload(
                image,
                review.getImagePublicId()
        );

        if (uploaded != null) {
            review.setImageUrl(uploaded.url());
            review.setImagePublicId(uploaded.publicId());
        }

        replaceVideo(review, video, form.isRemoveVideo());
        reviews.saveAndFlush(review);
    }

    private Review ownReview(Long memberId, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "ID đánh giá không hợp lệ."
            );
        }

        return reviews.findByIdAndUser_Id(id, memberId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy đánh giá của bạn."
                        )
                );
    }

    private User lockedMember(String email) {
        Long userId = accounts.requireMemberId(email);

        return users.lockByEmailForBooking(email)
                .filter(u -> u.getId().equals(userId))
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Tài khoản không hợp lệ."
                        )
                );
    }

    private void validateRating(ReviewForm form) {
        if (form == null
                || form.getRating() == null
                || form.getRating() < 1
                || form.getRating() > 5) {
            throw new IllegalArgumentException(
                    "Số sao phải từ 1 đến 5."
            );
        }

        if (form.getComment() != null
                && form.getComment().trim().length() > 2000) {
            throw new IllegalArgumentException(
                    "Nội dung tối đa 2000 ký tự."
            );
        }
    }

    private String clean(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }

    private void checkEvidence(
            Evidence ev,
            Long branchId,
            Long trainerId
    ) {
        if (branchId == null
                || branchId <= 0
                || !ev.branches.containsKey(branchId)) {
            throw new IllegalArgumentException(
                    "Chưa có bằng chứng sử dụng dịch vụ "
                            + "ở chi nhánh này."
            );
        }

        if (trainerId != null
                && (trainerId <= 0
                || !ev.trainerBranches
                .getOrDefault(trainerId, Set.of())
                .contains(branchId))) {
            throw new IllegalArgumentException(
                    "Chưa có bằng chứng tập với PT này "
                            + "tại chi nhánh đã chọn."
            );
        }
    }

    private Evidence evidence(Long memberId) {

        Map<Long, BranchOption> branchOptions =
                new LinkedHashMap<>();

        Map<String, TrainerOption> trainerOptions =
                new LinkedHashMap<>();

        Map<Long, Set<Long>> trainerBranches =
                new HashMap<>();

        // 1. Check-in đã check-out.
        List<Checkin> checkins =
                reviews.completedCheckins(memberId);

        for (Checkin checkin : checkins) {
            addBranch(
                    branchOptions,
                    checkin.getBranch()
            );
        }

        // 2. Lớp đã điểm danh ATTENDED.
        for (ClassRegistration r : reviews.attendedClasses(
                memberId, RegistrationStatus.ATTENDED
        )) {
            FitnessClass fc =
                    r.getSchedule().getFitnessClass();

            addBranch(branchOptions, fc.getBranch());

            addTrainer(
                    trainerOptions,
                    trainerBranches,
                    fc.getTrainer(),
                    fc.getBranch()
            );
        }

        // 3. PT 1:1 đã diễn ra, có Check-in
        // giao với khoảng thời gian buổi tập.
        LocalDateTime now = LocalDateTime.now();

        for (PersonalTrainingSchedule session :
                reviews.personalSessions(memberId)) {

            if (session.getStatus() != PersonalTrainingStatus.COMPLETED) {
                continue;
            }
            Trainer t = session.getTrainer();
            Branch branch = session.getBranch() != null ? session.getBranch() : t.getBranch();

            if (branch == null
                    || session.getEndTime() == null
                    || session.getStartTime() == null) {
                continue;
            }

            LocalDateTime from =
                    session.getScheduleDate()
                            .atTime(session.getStartTime());

            LocalDateTime to =
                    session.getScheduleDate()
                            .atTime(session.getEndTime());

            if (!to.isBefore(now)) {
                continue;
            }

            boolean present = checkins.stream().anyMatch(c ->
                    c.getBranch().getId()
                            .equals(branch.getId())
                            && c.getCheckInAt().isBefore(to)
                            && c.getCheckOutAt().isAfter(from)
            );

            if (present) {
                addBranch(branchOptions, branch);
                addTrainer(
                        trainerOptions,
                        trainerBranches,
                        t,
                        branch
                );
            }
        }

        return new Evidence(
                branchOptions,
                trainerOptions,
                trainerBranches
        );
    }

    private void addBranch(
            Map<Long, BranchOption> options,
            Branch b
    ) {
        if (b != null) {
            options.putIfAbsent(
                    b.getId(),
                    new BranchOption(
                            b.getId(), b.getName()
                    )
            );
        }
    }

    private void addTrainer(
            Map<String, TrainerOption> options,
            Map<Long, Set<Long>> trainerBranches,
            Trainer t,
            Branch b
    ) {
        if (t == null || b == null) {
            return;
        }

        String key = t.getId() + ":" + b.getId();

        options.putIfAbsent(
                key,
                new TrainerOption(
                        t.getId(),
                        t.getUser().getFullName(),
                        b.getId()
                )
        );

        trainerBranches
                .computeIfAbsent(
                        t.getId(),
                        k -> new HashSet<>()
                )
                .add(b.getId());
    }

    private ReviewView toView(Review r) {
        return new ReviewView(
                r.getId(),
                r.getBranch().getName(),
                r.getTrainer() == null
                        ? null
                        : r.getTrainer().getUser().getFullName(),
                r.getRating(),
                r.getComment(),
                r.getImageUrl(),
                r.getVideoUrl(),
                Boolean.TRUE.equals(r.getVisible()),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private record Evidence(
            Map<Long, BranchOption> branches,
            Map<String, TrainerOption> trainers,
            Map<Long, Set<Long>> trainerBranches
    ) {
        ReviewTargets targets() {
            return new ReviewTargets(
                    List.copyOf(branches.values()),
                    List.copyOf(trainers.values())
            );
        }
    }

    private void replaceVideo(Review review, MultipartFile video, boolean remove) {
        boolean uploading = video != null && !video.isEmpty();
        if (!uploading && !remove) {
            return;
        }
        if (uploading && remove) {
            throw new IllegalArgumentException("Chọn thay video hoặc xóa video, không chọn cả hai.");
        }
        String oldId = review.getVideoPublicId();
        MediaUploadResult uploaded = uploading ? cloudinary.uploadVideo(video, "utefit/reviews/videos") : null;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String toDelete = status == STATUS_COMMITTED ? oldId : uploaded == null ? null : uploaded.publicId();
                if (toDelete != null && !toDelete.isBlank()) {
                    try {
                        cloudinary.deleteVideo(toDelete);
                    } catch (RuntimeException ex) {
                        log.warn("Không dọn được video Review {}", toDelete, ex);
                    }
                }
            }
        });
        review.setVideoUrl(uploaded == null ? null : uploaded.url());
        review.setVideoPublicId(uploaded == null ? null : uploaded.publicId());
    }

    private MediaUploadResult upload(
            MultipartFile image,
            String oldPublicId
    ) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        MediaUploadResult uploaded;

        try {
            uploaded = cloudinary.uploadImage(
                    image,
                    "utefit/reviews"
            );
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Không thể tải ảnh lên Cloudinary. "
                            + "Thử lại sau.",
                    ex
            );
        }

        if (uploaded.url().length() > 500
                || uploaded.publicId().length() > 255) {

            try {
                cloudinary.delete(uploaded.publicId());
            } catch (RuntimeException ex) {
                log.warn(
                        "Không dọn được ảnh không hợp lệ",
                        ex
                );
            }

            throw new IllegalStateException(
                    "Đường dẫn ảnh vượt giới hạn cho phép."
            );
        }

        // Database rollback => xóa ảnh mới.
        // Database commit => xóa ảnh cũ.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {

                        String toDelete =
                                status == STATUS_COMMITTED
                                        ? oldPublicId
                                        : uploaded.publicId();

                        if (toDelete == null
                                || toDelete.isBlank()) {
                            return;
                        }

                        try {
                            cloudinary.delete(toDelete);
                        } catch (RuntimeException ex) {
                            log.warn(
                                    "Không xóa được ảnh Cloudinary {}",
                                    toDelete,
                                    ex
                            );
                        }
                    }
                }
        );

        return uploaded;
    }
}
