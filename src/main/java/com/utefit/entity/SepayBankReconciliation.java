package com.utefit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sepay_bank_reconciliations", uniqueConstraints = @UniqueConstraint(name = "uk_sepay_bank_claim",
        columnNames = {"environment", "gateway", "account_number", "bank_reference"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SepayBankReconciliation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_id", nullable = false, updatable = false) private Payment payment;
    @Column(nullable = false, length = 4, updatable = false) private String environment;
    @Column(nullable = false, length = 100, updatable = false) private String gateway;
    @Column(name = "account_number", nullable = false, length = 50, updatable = false) private String accountNumber;
    @Column(name = "sub_account", length = 50, updatable = false) private String subAccount;
    @Column(name = "bank_reference", nullable = false, length = 100, updatable = false) private String bankReference;
    @Column(nullable = false, precision = 15, scale = 2, updatable = false) private BigDecimal amount;
    @Column(name = "bank_date", nullable = false, updatable = false) private LocalDateTime bankDate;
    @Column(nullable = false, length = 40, updatable = false) private String method;
    @Column(nullable = false, length = 20) private String outcome;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "verified_by", updatable = false) private User verifiedBy;
    @Column(name = "verified_at", nullable = false, updatable = false) private LocalDateTime verifiedAt;
    @Nationalized @Column(length = 2000, updatable = false) private String evidence;
    @Nationalized @Column(length = 1000, updatable = false) private String reason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resolved_by") private User resolvedBy;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @Column(name = "refund_reference", length = 100) private String refundReference;
    @Column(name = "refunded_at") private LocalDateTime refundedAt;
    @Nationalized @Column(name = "resolution_evidence", length = 3000) private String resolutionEvidence;
}
