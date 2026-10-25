
package com.utefit.repository;

import com.utefit.entity.Payment;
import com.utefit.enums.PaymentMethod;
import com.utefit.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionCode(String transactionCode);

    @EntityGraph(attributePaths = {
            "membership", "membership.plan"
    })
    List<Payment> findByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    List<Payment> findByMembership_IdAndStatus(Long membershipId, PaymentStatus status);

    boolean existsByMembership_IdAndStatus(
            Long membershipId,
            PaymentStatus status
    );

    @EntityGraph(attributePaths = {
            "user", "membership", "membership.plan",
            "membership.branch"
    })
    List<Payment> findByStatusAndMethodOrderByCreatedAtAsc(
            PaymentStatus status,
            PaymentMethod method
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM Payment p
            WHERE p.id = :id
            """)
    Optional<Payment> lockById(
            @Param("id") Long id
    );
}
