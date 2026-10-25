package com.utefit.repository;

import com.utefit.entity.SepayPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface SepayPaymentDetailsRepository extends JpaRepository<SepayPaymentDetails, Long> {
    interface Target {
        Long getPaymentId();
        Long getMembershipId();
        Long getUserId();
    }
    @Query("select d.payment.id as paymentId, d.payment.membership.id as membershipId, d.payment.user.id as userId from SepayPaymentDetails d where d.paymentReference = :reference")
    Optional<Target> findTargetByReference(String reference);
}
