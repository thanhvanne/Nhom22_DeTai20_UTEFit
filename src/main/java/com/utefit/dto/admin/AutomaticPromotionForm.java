package com.utefit.dto.admin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@com.utefit.validation.ConsistentForm
public class AutomaticPromotionForm {
    @com.utefit.validation.ValidInput("version")
    private Long version;
    private Boolean active;
    @com.utefit.validation.ValidInput("percent")
    private BigDecimal percent;
    @com.utefit.validation.ValidInput("discountCap")
    private BigDecimal discountCap;
    @com.utefit.validation.ValidInput("minimumMonths")
    private Integer minimumMonths;
    @com.utefit.validation.ValidInput("renewalGraceDays")
    private Integer renewalGraceDays;
    @com.utefit.validation.ValidInput("valueThreshold")
    private BigDecimal valueThreshold;
    @com.utefit.validation.ValidInput("valueDiscount")
    private BigDecimal valueDiscount;
    @com.utefit.validation.ValidInput("premiumThreshold")
    private BigDecimal premiumThreshold;
    @com.utefit.validation.ValidInput("premiumDiscount")
    private BigDecimal premiumDiscount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate endDate;
}
