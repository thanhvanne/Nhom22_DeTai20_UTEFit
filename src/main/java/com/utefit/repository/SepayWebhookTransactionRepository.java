package com.utefit.repository;

import com.utefit.entity.SepayWebhookTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SepayWebhookTransactionRepository extends JpaRepository<SepayWebhookTransaction, Long> {
    java.util.List<SepayWebhookTransaction> findTop20ByPayment_IdOrderByReceivedAtDesc(Long paymentId);
    @org.springframework.data.jpa.repository.Query("select w from SepayWebhookTransaction w where w.environment = :environment and upper(w.gateway) = :gateway and w.accountNumber = :account and upper(w.bankReference) = :reference")
    Optional<SepayWebhookTransaction> findBankReference(@org.springframework.data.repository.query.Param("environment") String environment,
            @org.springframework.data.repository.query.Param("gateway") String gateway,
            @org.springframework.data.repository.query.Param("account") String account,
            @org.springframework.data.repository.query.Param("reference") String reference);
    @org.springframework.data.jpa.repository.Query("select count(w) from SepayWebhookTransaction w where w.payment.id = :paymentId and w.outcome = 'REVIEW' and (w.bankReference is null or upper(w.bankReference) <> :reference or w.conflictHash is not null or w.amount <> w.payment.amount or w.transferType <> 'in' or w.paymentReference is null or w.paymentReference <> w.payment.transactionCode or exists (select d.id from SepayPaymentDetails d where d.payment = w.payment and (d.accountNumber <> w.accountNumber or upper(d.gateway) <> upper(w.gateway) or coalesce(d.vaNumber, '') <> coalesce(w.subAccount, ''))))")
    long countOtherReviews(@org.springframework.data.repository.query.Param("paymentId") Long paymentId,
            @org.springframework.data.repository.query.Param("reference") String reference);
    boolean existsByPayment_IdAndOutcome(Long paymentId, String outcome);
    boolean existsByPayment_IdAndOutcomeAndIdNot(Long paymentId, String outcome, Long id);
    Optional<SepayWebhookTransaction> findByEnvironmentAndTransactionId(String environment, Long transactionId);
    Optional<SepayWebhookTransaction> findByEnvironmentAndGatewayAndAccountNumberAndBankReference(String environment, String gateway, String accountNumber, String bankReference);
    @EntityGraph(attributePaths = "payment")
    Page<SepayWebhookTransaction> findAllByOrderByReceivedAtDescIdDesc(Pageable pageable);
}
