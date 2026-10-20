package com.utefit.repository;

import com.utefit.entity.TrainerRequest;
import com.utefit.enums.TrainerRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerRequestRepository extends JpaRepository<TrainerRequest, Long> {

    boolean existsByMember_IdAndTrainer_IdAndStatus(
            Long memberId,
            Long trainerId,
            TrainerRequestStatus status
    );

    Page<TrainerRequest> findByMember_IdOrderByRequestedAtDesc(
            Long memberId,
            Pageable pageable
    );

    Page<TrainerRequest> findByTrainer_IdOrderByRequestedAtDesc(
            Long trainerId,
            Pageable pageable
    );

    Page<TrainerRequest> findByTrainer_User_IdOrderByRequestedAtDesc(
            Long trainerUserId,
            Pageable pageable
    );

    Optional<TrainerRequest> findByIdAndTrainer_User_Id(
            Long id,
            Long trainerUserId
    );

    // === Thêm cho dashboard ===
    long countByTrainer_User_IdAndStatus(Long trainerUserId,
                                         TrainerRequestStatus status);

    List<TrainerRequest> findTop5ByTrainer_User_IdAndStatusOrderByRequestedAtDesc(
            Long trainerUserId,
            TrainerRequestStatus status
    );
}