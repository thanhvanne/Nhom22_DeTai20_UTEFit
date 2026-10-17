package com.utefit.repository;

import com.utefit.entity.Trainer;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    @Query("""
            SELECT t FROM Trainer t
            JOIN FETCH t.user u
            LEFT JOIN FETCH t.branch b
            WHERE t.id = :id
              AND t.active = true
              AND u.enabled = true
              AND u.status = :status
              AND u.role = :role
              AND (b IS NULL OR b.active = true)
            """)
    Optional<Trainer> findPublicTrainerById(
            @Param("id") Long id,
            @Param("status") UserStatus status,
            @Param("role") UserRole role
    );

    @EntityGraph(attributePaths = {"user", "branch"})
    @Query(
            value = """
                    SELECT t FROM Trainer t
                    JOIN t.user u
                    LEFT JOIN t.branch b
                    WHERE t.active = true
                      AND u.enabled = true
                      AND u.status = :status
                      AND u.role = :role
                      AND (b IS NULL OR b.active = true)
                      AND (
                          :keyword IS NULL
                          OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(CONCAT(u.firstName, ' ', u.lastName))
                             LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(t.specialization)
                             LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                      AND (:branchId IS NULL OR b.id = :branchId)
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Trainer t
                    JOIN t.user u
                    LEFT JOIN t.branch b
                    WHERE t.active = true
                      AND u.enabled = true
                      AND u.status = :status
                      AND u.role = :role
                      AND (b IS NULL OR b.active = true)
                      AND (
                          :keyword IS NULL
                          OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(CONCAT(u.firstName, ' ', u.lastName))
                             LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(t.specialization)
                             LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                      AND (:branchId IS NULL OR b.id = :branchId)
                    """
    )
    Page<Trainer> searchPublicTrainers(
            @Param("keyword") String keyword,
            @Param("branchId") Long branchId,
            @Param("status") UserStatus status,
            @Param("role") UserRole role,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Trainer t
            JOIN FETCH t.user u
            LEFT JOIN FETCH t.branch
            WHERE u.id = :userId
              AND u.role = :role
            """)
    Optional<Trainer> findByUserId(
            @Param("userId") Long userId,
            @Param("role") UserRole role
    );
}