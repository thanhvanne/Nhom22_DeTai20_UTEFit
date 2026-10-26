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
    @Value("${SEPAY_LIVE_ACCEPT_NEW_PAYMENTS:false}") private boolean liveAcceptNewPayments;
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
    @Value("${SEPAY_LIVE_BANK_CODE:}") private String liveBankCode;
    @Value("${SEPAY_LIVE_GATEWAY:}") private String liveGateway;
    @Value("${SEPAY_LIVE_ACCOUNT_NUMBER:}") private String liveAccountNumber;
    @Value("${SEPAY_LIVE_VA_NUMBER:}") private String liveVaNumber = "";
    @Value("${SEPAY_LIVE_ACCOUNT_NAME:}") private String liveAccountName;
    @Value("${SEPAY_LIVE_DATABASE_NAME:}") private String liveDatabaseName;
    @Value("${SEPAY_LIVE_DEPLOYMENT_ID:}") private String liveDeploymentId;
    @Value("${SEPAY_LIVE_APPROVAL:}") private String liveApproval;
    @Value("${SEPAY_LIVE_PUBLIC_URL:}") private String livePublicUrl;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment springEnvironment;

    public String getBankCode() { return testMode() ? bankCode : liveBankCode; }
    public String getGateway() { return testMode() ? gateway : liveGateway; }
    public String getAccountNumber() { return testMode() ? accountNumber : liveAccountNumber; }
    public String getAccountName() { return testMode() ? accountName : liveAccountName; }
    public String getVaNumber() { return testMode() ? "" : liveVaNumber; }
    public boolean acceptingNewPayments() { return testMode() || liveAcceptNewPayments; }

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
        if (!enabled) throw new IllegalStateException("SePay chưa được bật. Vui lòng dùng CASH hoặc liên hệ quản trị viên.");
        if (!"hmac".equalsIgnoreCase(webhookAuth) || !(testMode() || "LIVE".equals(mode()))
                || !getBankCode().matches("[A-Za-z0-9]{2,32}") || getGateway().isBlank() || getGateway().length() > 100
                || !getAccountNumber().matches("[A-Za-z0-9]{3,50}") || getAccountName().isBlank() || getAccountName().length() > 150
                || !paymentPrefix.matches("[A-Z]{2,5}") || paymentMinutes < 5 || paymentMinutes > 60
                || secret().isBlank() || secret().length() < 32 || placeholder(getBankCode()) || placeholder(getGateway()) || placeholder(getAccountNumber())
                || placeholder(getAccountName()) || placeholder(secret())) {
            throw new IllegalStateException("Cấu hình SePay chưa đầy đủ hoặc không hợp lệ. Liên hệ quản trị viên.");
        }
        if (!testMode() && (!liveEnabled || liveWebhookSecret.equals(webhookSecret))) {
            throw new IllegalStateException("SePay Live cần được bật rõ ràng và dùng secret riêng với Test.");
        }
        if (!testMode()) requireLiveDeployment();
        if (!testMode() && ((!liveVaNumber.isEmpty() && (!liveVaNumber.matches("[A-Za-z0-9]{3,50}")
                || placeholder(liveVaNumber) || liveVaNumber.equals(liveAccountNumber)))
                || (("BIDV".equalsIgnoreCase(getBankCode()) || "BIDV".equalsIgnoreCase(getGateway())) && liveVaNumber.isEmpty()))) {
            throw new IllegalStateException("SePay Live BIDV cần VA riêng hợp lệ; tài khoản chính chỉ dùng đối chiếu Webhook.");
        }
        if (testMode()) {
            var catalogs = Pattern.compile("(?i)(?:^|;)databaseName=([^;]+)").matcher(databaseUrl).results().toList();
            if (testDatabaseName.isBlank() || !testDatabaseName.toLowerCase(Locale.ROOT).matches(".*(qa|test).*")
                    || catalogs.size() != 1 || !catalogs.get(0).group(1).equalsIgnoreCase(testDatabaseName)) {
                throw new IllegalStateException("SePay Test chỉ được dùng SQL database QA/Test riêng, khớp SEPAY_TEST_DATABASE_NAME.");
            }
        }
    }

    private void requireLiveDeployment() {
        var profiles = java.util.Arrays.asList(springEnvironment.getActiveProfiles());
        var catalogs = Pattern.compile("(?i)(?:^|;)databaseName=([^;]+)").matcher(databaseUrl).results().toList();
        java.net.URI publicUrl;
        try { publicUrl = java.net.URI.create(livePublicUrl); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("Invalid Live HTTPS origin"); }
        if (!profiles.contains("sepay") || !profiles.contains("sepay-live")
                || profiles.stream().anyMatch(profile -> profile.equals("qa") || profile.equals("test"))
                || !"I_APPROVE_SEPAY_LIVE".equals(liveApproval)
                || !liveDatabaseName.matches("[A-Za-z0-9_]{3,128}")
                || liveDatabaseName.toLowerCase(Locale.ROOT).matches(".*(qa|test).*")
                || liveDatabaseName.equalsIgnoreCase(testDatabaseName)
                || catalogs.size() != 1 || !catalogs.get(0).group(1).equalsIgnoreCase(liveDatabaseName)
                || !liveDeploymentId.matches("[A-Za-z0-9_-]{16,100}") || placeholder(liveDeploymentId)
                || !"https".equals(publicUrl.getScheme()) || publicUrl.getHost() == null
                || !publicUrl.getHost().matches("(?i)[a-z0-9][a-z0-9.-]*\\.[a-z]{2,63}") || publicUrl.getUserInfo() != null
                || publicUrl.getQuery() != null || publicUrl.getFragment() != null
                || !(publicUrl.getPath().isEmpty() || publicUrl.getPath().equals("/"))
                || !"validate".equals(springEnvironment.getProperty("spring.jpa.hibernate.ddl-auto"))
                || !"never".equals(springEnvironment.getProperty("spring.sql.init.mode"))
                || !springEnvironment.getProperty("utefit.jwt.cookie-secure", Boolean.class, false)) {
            throw new IllegalStateException("SePay Live bị chặn: cần profile, HTTPS, database, cookie và phê duyệt triển khai hợp lệ.");
        }
    }

    private boolean placeholder(String value) {
        return value.toUpperCase(Locale.ROOT).startsWith("YOUR_") || value.startsWith("REPLACE_");
    }
}
