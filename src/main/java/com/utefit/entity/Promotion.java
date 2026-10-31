
package com.utefit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_promotions_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {
    @Column(name = "min_order_amount", precision = 15, scale = 2) private BigDecimal minOrderAmount;
    @Column(name = "max_discount_amount", precision = 15, scale = 2) private BigDecimal maxDiscountAmount;
    @Column(name = "per_user_limit") private Integer perUserLimit;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "discount_percent",
            nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null) active = true;
    }
}
