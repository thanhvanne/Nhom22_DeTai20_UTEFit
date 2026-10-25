package com.utefit.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class VnpayProperties {
    @Value("${utefit.vnpay.enabled:${VNPAY_ENABLED:false}}")
    private boolean enabled;
    @Value("${utefit.vnpay.tmn-code:${VNPAY_TMN_CODE:}}")
    private String tmnCode;
    @Value("${utefit.vnpay.hash-secret:${VNPAY_HASH_SECRET:}}")
    private String hashSecret;
    @Value("${utefit.vnpay.return-url:${VNPAY_RETURN_URL:http://localhost:8080/payments/vnpay/return}}")
    private String returnUrl;

    public void requireConfigured() {
        if (!enabled || tmnCode == null || tmnCode.isBlank()
                || hashSecret == null || hashSecret.isBlank()) {
            throw new IllegalStateException("Thanh toán VNPay Sandbox chưa được cấu hình. Bạn vẫn có thể chọn CASH.");
        }
    }
}
