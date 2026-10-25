package com.utefit.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@Getter
public class SepayProperties {
    @Value("${SEPAY_ENABLED:false}") private boolean enabled;
    @Value("${SEPAY_ENVIRONMENT:test}") private String environment;
    @Value("${SEPAY_LIVE_ENABLED:false}") private boolean liveEnabled;
    @Value("${SEPAY_TEST_QR_PREVIEW:false}") private boolean testQrPreview;
    @Value("${SEPAY_BANK_CODE:}") private String bankCode;
    @Value("${SEPAY_GATEWAY:${SEPAY_BANK_CODE:}}") private String gateway;
    @Value("${SEPAY_ACCOUNT_NUMBER:}") private String accountNumber;
    @Value("${SEPAY_ACCOUNT_NAME:}") private String accountName;
    @Value("${SEPAY_WEBHOOK_SECRET:}") private String webhookSecret;
    @Value("${SEPAY_LIVE_WEBHOOK_SECRET:}") private String liveWebhookSecret;
    @Value("${SEPAY_WEBHOOK_AUTH:hmac}") private String webhookAuth;
    @Value("${SEPAY_PAYMENT_PREFIX:UTE}") private String paymentPrefix;
    @Value("${SEPAY_PAYMENT_MINUTES:15}") private int paymentMinutes;
    @Value("${SEPAY_TEST_DATABASE_NAME:}") private String testDatabaseName;
    @Value("${spring.datasource.url:}") private String databaseUrl;

    public String mode() {
        return environment.toUpperCase(Locale.ROOT);
    }

    public boolean testMode() {
        return "TEST".equals(mode());
    }

    public String secret() {
        return testMode() ? webhookSecret : liveWebhookSecret;
    }

    public void requireConfigured() {
        if (!enabled) throw new IllegalStateException("SePay chưa được bật. CASH và VNPay vẫn được giữ nguyên.");
        if (!"hmac".equalsIgnoreCase(webhookAuth) || !(testMode() || "LIVE".equals(mode()))
                || !bankCode.matches("[A-Za-z0-9]{2,32}") || gateway.isBlank() || gateway.length() > 100
                || !accountNumber.matches("[A-Za-z0-9]{3,50}") || accountName.isBlank() || accountName.length() > 150
                || !paymentPrefix.matches("[A-Z]{2,5}") || paymentMinutes < 5 || paymentMinutes > 60
                || secret().isBlank() || secret().length() < 32 || placeholder(bankCode) || placeholder(gateway) || placeholder(accountNumber)
                || placeholder(accountName) || placeholder(secret())) {
            throw new IllegalStateException("Cấu hình SePay chưa đầy đủ hoặc không hợp lệ. Liên hệ quản trị viên.");
        }
        if (!testMode() && (!liveEnabled || liveWebhookSecret.equals(webhookSecret))) {
            throw new IllegalStateException("SePay Live cần được bật rõ ràng và dùng secret riêng với Test.");
        }
        if (testMode()) {
            var catalogs = Pattern.compile("(?i)(?:^|;)databaseName=([^;]+)").matcher(databaseUrl).results().toList();
            if (testDatabaseName.isBlank() || !testDatabaseName.toLowerCase(Locale.ROOT).matches(".*(qa|test).*")
                    || catalogs.size() != 1 || !catalogs.get(0).group(1).equalsIgnoreCase(testDatabaseName)) {
                throw new IllegalStateException("SePay Test chỉ được dùng SQL database QA/Test riêng, khớp SEPAY_TEST_DATABASE_NAME.");
            }
        }
    }

    private boolean placeholder(String value) {
        return value.toUpperCase(Locale.ROOT).startsWith("YOUR_") || value.startsWith("REPLACE_");
    }
}
