
package com.utefit.repository;

import com.utefit.entity.Membership;
import com.utefit.enums.MembershipStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository
        extends JpaRepository<Membership, Long> {

    @EntityGraph(attributePaths = {"branch", "plan"})
    List<Membership> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    boolean existsByUser_IdAndStatus(
            Long userId,
            MembershipStatus status
    );

    boolean existsByUser_IdAndStatusAndEndDateGreaterThanEqual(
            Long userId,
            MembershipStatus status,
            LocalDate date
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m FROM Membership m
            WHERE m.id = :id
            """)
    Optional<Membership> lockById(
            @Param("id") Long id
    );
}
