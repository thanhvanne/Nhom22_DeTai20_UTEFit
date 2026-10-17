package com.utefit.entity;

import com.utefit.enums.OtpType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpType type;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private Boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (used == null) {
            used = false;
        }

        if (attemptCount == null) {
            attemptCount = 0;
        }
    }
}