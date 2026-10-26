package com.utefit.repository;

import com.utefit.entity.Membership;
import com.utefit.enums.MembershipStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository
        extends JpaRepository<Membership, Long> {
    @EntityGraph(attributePaths = {"branch", "plan", "branchIds"})
    @Query("select distinct m from Membership m where m.user.id = :userId and m.status = com.utefit.enums.MembershipStatus.ACTIVE and (exists (select p.id from Payment p where p.membership = m and p.status = com.utefit.enums.PaymentStatus.PAID) or (m.quotedScope is null and not exists (select p.id from Payment p where p.membership = m)))")
    List<Membership> findPaidAccessByUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"branch", "plan"})
    List<Membership> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    @Query("""
       SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Membership m
       WHERE m.branch.id = :branchId AND m.status = :status
         AND m.startDate <= :today AND m.endDate >= :today
    """)
    boolean hasActiveMembersForBranch(@Param("branchId") Long branchId,
                                      @Param("status") MembershipStatus status,
                                      @Param("today") LocalDate today);

    List<Membership> findByStatusAndCreatedAtBefore(MembershipStatus status, LocalDateTime before);

    boolean existsByUser_IdAndStatus(
            Long userId,
            MembershipStatus status
    );

    boolean existsByUser_IdAndStatusAndEndDateGreaterThanEqual(
            Long userId,
            MembershipStatus status,
            LocalDate date
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Membership m SET m.status = :expired WHERE m.status = :active AND m.endDate < :today")
    int expireOutdated(@Param("active") MembershipStatus active,
                       @Param("expired") MembershipStatus expired,
                       @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m FROM Membership m
            WHERE m.id = :id
            """)
    Optional<Membership> lockById(
            @Param("id") Long id
    );
}
