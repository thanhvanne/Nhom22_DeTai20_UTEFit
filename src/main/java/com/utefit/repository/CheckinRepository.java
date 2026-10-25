
package com.utefit.repository;

import com.utefit.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckinRepository
        extends JpaRepository<Checkin, Long> {

    boolean existsByUser_IdAndCheckOutAtIsNull(Long userId);

    @EntityGraph(attributePaths = {"branch"})
    Optional<Checkin> findFirstByUser_IdAndCheckOutAtIsNullOrderByCheckInAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"branch"})
    Optional<Checkin> findFirstByUser_IdOrderByCheckInAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"branch"})
    List<Checkin> findByUser_IdOrderByCheckInAtDesc(Long userId);

    @Query("""
      SELECT c FROM Checkin c WHERE c.user.id = :memberId AND c.branch.id = :branchId
        AND c.checkOutAt IS NOT NULL AND c.checkInAt < :sessionEnd AND c.checkOutAt > :sessionStart
    """)
    List<Checkin> findCompletedDuringSession(@Param("memberId") Long memberId,
                                             @Param("branchId") Long branchId,
                                             @Param("sessionStart") LocalDateTime sessionStart,
                                             @Param("sessionEnd") LocalDateTime sessionEnd);

    @EntityGraph(attributePaths = {"user", "branch"})
    @Query(
            value = """
            SELECT c FROM Checkin c
            WHERE (:branchId IS NULL OR c.branch.id = :branchId)
              AND (:email IS NULL OR LOWER(c.user.email) LIKE :email)
              AND (:start IS NULL OR c.checkInAt >= :start)
              AND (:end IS NULL OR c.checkInAt < :end)
              AND (:open IS NULL
                   OR (:open = true AND c.checkOutAt IS NULL)
                   OR (:open = false AND c.checkOutAt IS NOT NULL))
            """,
            countQuery = """
            SELECT COUNT(c) FROM Checkin c
            WHERE (:branchId IS NULL OR c.branch.id = :branchId)
              AND (:email IS NULL OR LOWER(c.user.email) LIKE :email)
              AND (:start IS NULL OR c.checkInAt >= :start)
              AND (:end IS NULL OR c.checkInAt < :end)
              AND (:open IS NULL
                   OR (:open = true AND c.checkOutAt IS NULL)
                   OR (:open = false AND c.checkOutAt IS NOT NULL))
            """
    )
    Page<Checkin> search(
            @Param("branchId") Long branchId,
            @Param("email") String email,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("open") Boolean open,
            Pageable pageable
    );
}
