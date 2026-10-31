package com.utefit.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "utefit.loyalty")
@Getter
@Setter
public class LoyaltyProperties {
    private boolean enabled = false;
    private BigDecimal silverThreshold = new BigDecimal("500000");
    private BigDecimal goldThreshold = new BigDecimal("1500000");
    private BigDecimal silverPercent = new BigDecimal("5");
    private BigDecimal goldPercent = new BigDecimal("10");
    private BigDecimal welcomePercent = new BigDecimal("10");
    private BigDecimal welcomeCap = new BigDecimal("50000");
    private int loyaltyMonths = 6;
    private int renewalGraceDays = 30;
    private BigDecimal renewalPercent = new BigDecimal("10");
    private BigDecimal valueThreshold = new BigDecimal("500000");
    private BigDecimal valueDiscount = new BigDecimal("50000");
    private BigDecimal premiumThreshold = new BigDecimal("1000000");
    private BigDecimal premiumDiscount = new BigDecimal("100000");

    @PostConstruct
    public void validate() {
        for (BigDecimal amount : new BigDecimal[]{silverThreshold, goldThreshold, welcomeCap, valueThreshold, valueDiscount, premiumThreshold, premiumDiscount}) {
            if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) throw new IllegalArgumentException("Ngưỡng/quyền lợi hội viên phải là số VND nguyên dương.");
        }
        for (BigDecimal percent : new BigDecimal[]{silverPercent, goldPercent, welcomePercent, renewalPercent}) {
            if (percent == null || percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) >= 0 || percent.scale() > 2) throw new IllegalArgumentException("Phần trăm ưu đãi phải lớn hơn 0 và nhỏ hơn 100.");
        }
        if (goldThreshold.compareTo(silverThreshold) <= 0 || premiumThreshold.compareTo(valueThreshold) <= 0 || loyaltyMonths < 1 || renewalGraceDays < 0
                || goldPercent.compareTo(silverPercent) < 0 || premiumDiscount.compareTo(valueDiscount) < 0 || valueDiscount.compareTo(valueThreshold) >= 0 || premiumDiscount.compareTo(premiumThreshold) >= 0) throw new IllegalArgumentException("Cấu hình phân hạng/ưu đãi không hợp lệ.");
    }
}
