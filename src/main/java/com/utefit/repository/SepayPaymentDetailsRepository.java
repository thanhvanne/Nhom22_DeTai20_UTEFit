package com.utefit.repository;

import com.utefit.entity.SepayPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface SepayPaymentDetailsRepository extends JpaRepository<SepayPaymentDetails, Long> {
    @Query("select d.id from SepayPaymentDetails d where d.payment.user.id = :userId and d.environment = 'LIVE'")
    java.util.Set<Long> livePaymentIds(@org.springframework.data.repository.query.Param("userId") Long userId);
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"payment", "payment.membership"})
    Optional<SepayPaymentDetails> findById(Long id);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"payment", "payment.user", "payment.membership"})
    @org.springframework.data.jpa.repository.Query("select d from SepayPaymentDetails d where d.environment = 'LIVE' and (d.payment.status = com.utefit.enums.PaymentStatus.PENDING or d.payment.status = com.utefit.enums.PaymentStatus.CANCEL_REQUESTED or exists (select w.id from SepayWebhookTransaction w where w.payment = d.payment and w.outcome = 'REVIEW')) order by d.payment.createdAt, d.id")
    org.springframework.data.domain.Page<SepayPaymentDetails> findReconciliationCases(org.springframework.data.domain.Pageable pageable);
    interface Target {
        Long getPaymentId();
        Long getMembershipId();
        Long getUserId();
    }
    @Query("select d.payment.id as paymentId, d.payment.membership.id as membershipId, d.payment.user.id as userId from SepayPaymentDetails d where d.paymentReference = :reference")
    Optional<Target> findTargetByReference(String reference);
}
