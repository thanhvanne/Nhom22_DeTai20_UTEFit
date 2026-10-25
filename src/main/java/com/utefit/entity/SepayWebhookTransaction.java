package com.utefit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sepay_webhook_transactions", uniqueConstraints = @UniqueConstraint(name = "uk_sepay_environment_transaction", columnNames = {"environment", "transaction_id"}),
        indexes = @Index(name = "idx_sepay_outcome_received", columnList = "outcome,received_at"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SepayWebhookTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 4) private String environment;
    @Column(name = "transaction_id", nullable = false) private Long transactionId;
    @Column(nullable = false, length = 100) private String gateway;
    @Column(name = "account_number", nullable = false, length = 50) private String accountNumber;
    @Column(name = "payment_reference", length = 100) private String paymentReference;
    @Column(name = "bank_reference", length = 100) private String bankReference;
    @Nationalized @Column(length = 2000) private String content;
    @Column(name = "transfer_type", nullable = false, length = 3) private String transferType;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(name = "transaction_date", nullable = false) private LocalDateTime transactionDate;
    @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;
    @Column(name = "body_hash", nullable = false, length = 64) private String bodyHash;
    @Column(nullable = false, length = 64) private String fingerprint;
    @Column(nullable = false, length = 10) private String outcome;
    @Column(nullable = false, length = 100) private String reason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_id") private Payment payment;
    @Column(name = "conflict_transaction_id") private Long conflictTransactionId;
    @Column(name = "conflict_hash", length = 64) private String conflictHash;
}
