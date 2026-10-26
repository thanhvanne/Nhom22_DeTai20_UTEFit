package com.utefit.repository;

import com.utefit.entity.SepayBankReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SepayBankReconciliationRepository extends JpaRepository<SepayBankReconciliation, Long> {
    Optional<SepayBankReconciliation> findByEnvironmentAndGatewayAndAccountNumberAndBankReference(
            String environment, String gateway, String accountNumber, String bankReference);
    List<SepayBankReconciliation> findByPayment_IdOrderByIdDesc(Long paymentId);
    boolean existsByRefundReference(String refundReference);
}
