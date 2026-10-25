package com.utefit.repository;

import com.utefit.entity.SepayWebhookTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SepayWebhookTransactionRepository extends JpaRepository<SepayWebhookTransaction, Long> {
    Optional<SepayWebhookTransaction> findByEnvironmentAndTransactionId(String environment, Long transactionId);
    Optional<SepayWebhookTransaction> findByEnvironmentAndGatewayAndAccountNumberAndBankReference(String environment, String gateway, String accountNumber, String bankReference);
    @EntityGraph(attributePaths = "payment")
    Page<SepayWebhookTransaction> findAllByOrderByReceivedAtDescIdDesc(Pageable pageable);
}
