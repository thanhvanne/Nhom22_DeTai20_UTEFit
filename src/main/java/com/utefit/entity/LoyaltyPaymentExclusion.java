package com.utefit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_payment_exclusions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyPaymentExclusion {
    @Id @Column(name = "payment_id") private Long paymentId;
    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 250) private String reason;
    @Column(name = "reviewed_at", nullable = false) private LocalDateTime reviewedAt;
}
