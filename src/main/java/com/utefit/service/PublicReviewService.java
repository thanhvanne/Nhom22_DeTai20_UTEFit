package com.utefit.service;

import com.utefit.entity.Review;
import com.utefit.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicReviewService {

    private final ReviewRepository reviews;

    public record PublicReview(
            Long id,
            String author,
            String branch,
            String trainer,
            Integer rating,
            String comment,
            String imageUrl,
            LocalDateTime createdAt
    ) {}

    @Transactional(readOnly = true)
    public Page<PublicReview> search(
            Long branchId,
            Integer rating,
            int page
    ) {
        if (rating != null
                && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException(
                    "Điểm lọc phải từ 1 đến 5."
            );
        }

        return reviews.searchVisible(
                branchId,
                rating,
                PageRequest.of(
                        Math.max(0, page),
                        9,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt",
                                "id"
                        )
                )
        ).map(this::view);
    }

    @Transactional(readOnly = true)
    public List<PublicReview> featured() {
        return reviews
                .findTop3ByVisibleTrueOrderByCreatedAtDescIdDesc()
                .stream()
                .map(this::view)
                .toList();
    }

    private PublicReview view(Review r) {
        String first = r.getUser().getFirstName();

        return new PublicReview(
                r.getId(),
                first == null || first.isBlank()
                        ? "Hội viên"
                        : first.trim(),
                r.getBranch().getName(),
                r.getTrainer() == null
                        ? null
                        : r.getTrainer().getUser().getFullName(),
                r.getRating(),
                r.getComment(),
                r.getImageUrl(),
                r.getCreatedAt()
        );
    }
}