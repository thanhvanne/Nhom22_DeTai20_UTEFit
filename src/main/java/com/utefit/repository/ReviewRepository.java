package com.utefit.repository;

import com.utefit.entity.*;
import com.utefit.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {
            "branch", "trainer", "trainer.user"
    })
    Page<Review> findByUser_IdOrderByCreatedAtDescIdDesc(
            Long userId, Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "branch", "trainer", "trainer.user"
    })
    Optional<Review> findByIdAndUser_Id(
            Long id, Long userId
    );

    boolean existsByUser_IdAndBranch_IdAndTrainerIsNull(
            Long userId, Long branchId
    );

    boolean existsByUser_IdAndTrainer_Id(
            Long userId, Long trainerId
    );

    @EntityGraph(attributePaths = {
            "branch", "trainer", "trainer.user", "user"
    })
    @Query(
            value = """
            SELECT r FROM Review r
            WHERE r.visible = true
              AND (:branchId IS NULL OR r.branch.id = :branchId)
              AND (:rating IS NULL OR r.rating = :rating)
            """,
            countQuery = """
            SELECT COUNT(r) FROM Review r
            WHERE r.visible = true
              AND (:branchId IS NULL OR r.branch.id = :branchId)
              AND (:rating IS NULL OR r.rating = :rating)
            """
    )
    Page<Review> searchVisible(
            @Param("branchId") Long branchId,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "branch", "trainer", "trainer.user", "user"
    })
    List<Review> findTop3ByVisibleTrueOrderByCreatedAtDescIdDesc();

    @Query("""
            SELECT c FROM Checkin c
            JOIN FETCH c.branch
            WHERE c.user.id = :memberId
              AND c.checkOutAt IS NOT NULL
            """)
    List<Checkin> completedCheckins(
            @Param("memberId") Long memberId
    );

    @Query("""
            SELECT r FROM ClassRegistration r
              JOIN FETCH r.schedule s
              JOIN FETCH s.fitnessClass fc
              JOIN FETCH fc.branch
              LEFT JOIN FETCH fc.trainer t
              LEFT JOIN FETCH t.user
            WHERE r.user.id = :memberId
              AND r.status = :status
            """)
    List<ClassRegistration> attendedClasses(
            @Param("memberId") Long memberId,
            @Param("status") RegistrationStatus status
    );

    @Query("""
            SELECT s FROM PersonalTrainingSchedule s
              JOIN FETCH s.trainer t
              JOIN FETCH t.user
              LEFT JOIN FETCH t.branch
            WHERE s.member.id = :memberId
              AND s.active = true
            """)
    List<PersonalTrainingSchedule> personalSessions(
            @Param("memberId") Long memberId
    );
}