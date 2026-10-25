package com.utefit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

@Entity
@Table(name = "sepay_payment_details", uniqueConstraints = @UniqueConstraint(name = "uk_sepay_payment_reference", columnNames = "payment_reference"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SepayPaymentDetails {
    @Id @Column(name = "payment_id") private Long id;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id") private Payment payment;
    @Column(name = "payment_reference", nullable = false, length = 35, updatable = false) private String paymentReference;
    @Column(nullable = false, length = 4, updatable = false) private String environment;
    @Column(name = "bank_code", nullable = false, length = 32, updatable = false) private String bankCode;
    @Column(nullable = false, length = 100, updatable = false) private String gateway;
    @Column(name = "account_number", nullable = false, length = 50, updatable = false) private String accountNumber;
    @Nationalized @Column(name = "account_name", nullable = false, length = 150, updatable = false) private String accountName;
    @Column(name = "expires_at", nullable = false, updatable = false) private LocalDateTime expiresAt;
}
