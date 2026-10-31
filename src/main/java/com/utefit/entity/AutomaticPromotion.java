package com.utefit.entity;

import com.utefit.enums.AutomaticPromotionKind;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "automatic_promotions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AutomaticPromotion {
    @Id @Enumerated(EnumType.STRING) @Column(length = 12) private AutomaticPromotionKind kind;
    @Column(nullable = false) private boolean active;
    @Column(name = "discount_percent", precision = 5, scale = 2) private BigDecimal percent;
    @Column(name = "discount_cap", precision = 15, scale = 2) private BigDecimal discountCap;
    @Column(name = "minimum_months") private Integer minimumMonths;
    @Column(name = "renewal_grace_days") private Integer renewalGraceDays;
    @Column(name = "value_threshold", precision = 15, scale = 2) private BigDecimal valueThreshold;
    @Column(name = "value_discount", precision = 15, scale = 2) private BigDecimal valueDiscount;
    @Column(name = "premium_threshold", precision = 15, scale = 2) private BigDecimal premiumThreshold;
    @Column(name = "premium_discount", precision = 15, scale = 2) private BigDecimal premiumDiscount;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 255) private String updatedBy;
    @Version @Column(nullable = false) private Long version;
}
