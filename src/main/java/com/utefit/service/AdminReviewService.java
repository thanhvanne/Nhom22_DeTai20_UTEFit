
package com.utefit.service;

import com.utefit.entity.Review;
import com.utefit.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final EntityManager entityManager;

    @Getter
    @AllArgsConstructor
    public static class ReviewRow {
        private Long id;
        private String memberEmail;
        private String branchName;
        private String trainerName;
        private Integer rating;
        private String comment;
        private Boolean visible;
        private LocalDateTime createdAt;
    }

    private ReviewRow toRow(Review review) {
        String trainerName = review.getTrainer() == null
                ? "-"
                : review.getTrainer().getUser().getFullName();

        return new ReviewRow(
                review.getId(),
                review.getUser().getEmail(),
                review.getBranch().getName(),
                trainerName,
                review.getRating(),
                review.getComment(),
                review.getVisible(),
                review.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<ReviewRow> search(
            Integer rating,
            Long branchId,
            int page
    ) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException(
                    "Rating phải nằm trong khoảng 1–5.");
        }

        String conditions = """
                WHERE (:rating IS NULL OR r.rating = :rating)
                  AND (:branchId IS NULL OR r.branch.id = :branchId)
                """;

        var query = entityManager.createQuery(
                "SELECT r FROM Review r " + conditions
                        + " ORDER BY r.createdAt DESC, r.id DESC",
                Review.class
        );

        var countQuery = entityManager.createQuery(
                "SELECT COUNT(r) FROM Review r " + conditions,
                Long.class
        );

        query.setParameter("rating", rating);
        query.setParameter("branchId", branchId);

        countQuery.setParameter("rating", rating);
        countQuery.setParameter("branchId", branchId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                10
        );

        List<ReviewRow> rows = query
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList()
                .stream()
                .map(this::toRow)
                .toList();

        return new PageImpl<>(
                rows, pageable, countQuery.getSingleResult());
    }

    @Transactional
    public void toggleVisibility(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy đánh giá."));

        review.setVisible(
                !Boolean.TRUE.equals(review.getVisible()));

        reviewRepository.save(review);
    }
}
