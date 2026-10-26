package com.utefit.entity;

import com.utefit.enums.PaymentMethod;
import com.utefit.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "membership_id",
            foreignKey = @ForeignKey(name = "fk_payment_membership")
    )
    private Membership membership;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Column(name = "gateway_transaction_no", length = 100)
    private String gatewayTransactionNo;

    @Column(name = "gateway_response_code", length = 10)
    private String gatewayResponseCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancellation_requested_at")
    private LocalDateTime cancellationRequestedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @org.hibernate.annotations.Nationalized
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @org.hibernate.annotations.Nationalized
    @Column(name = "late_gateway_evidence", length = 2000)
    private String lateGatewayEvidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
}
