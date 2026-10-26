package com.utefit.service;

import com.utefit.config.SepayProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

@Service
public class SepayReconciliationLookup {
    private final SepayProperties properties;
    private final SepayDatabaseGuard databaseGuard;
    private final HttpClient client;
    @Value("${SEPAY_LIVE_API_LOOKUP_ENABLED:false}") private boolean enabled;
    @Value("${SEPAY_LIVE_API_TOKEN:}") private String token;
    @Value("${SEPAY_LIVE_BANK_ACCOUNT_ID:}") private String bankAccountId;

    public SepayReconciliationLookup(SepayProperties properties, SepayDatabaseGuard databaseGuard) {
        this.properties = properties;
        this.databaseGuard = databaseGuard;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String lookup(LocalDate date, int page) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Admin được tra cứu giao dịch.");
        }
        databaseGuard.requireSafeDatabase();
        if (!enabled || properties.testMode() || token == null || token.isBlank() || token.length() > 4096
                || token.chars().anyMatch(character -> character <= 32 || character > 126)
                || token.startsWith("YOUR_") || !bankAccountId.matches("[a-fA-F0-9-]{36}")
                || date == null || date.isAfter(LocalDate.now(SepayPaymentService.ZONE)) || page < 1 || page > 10000) {
            throw new IllegalStateException("Tra cứu API Live chưa được bật/cấu hình hoặc tham số không hợp lệ. Dùng Dashboard SePay để đối soát.");
        }
        String query = "bank_account_id=" + bankAccountId + "&per_page=20&page=" + page
                + "&transaction_date_from=" + encode(date + " 00:00:00") + "&transaction_date_to=" + encode(date + " 23:59:59");
        var request = HttpRequest.newBuilder(URI.create("https://userapi.sepay.vn/v2/transactions?" + query))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token)
                .header("Accept", "application/json").GET().build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var stream = response.body()) {
                byte[] body = stream.readNBytes(262145);
                if (response.statusCode() != 200 || body.length > 262144) throw new IllegalStateException("API unavailable");
                var json = JsonMapper.builder().build().readTree(body);
                if (!"success".equals(json.path("status").asString()) || !json.path("data").isArray()
                        || json.path("data").size() > 20) throw new IllegalStateException("Unexpected API response");
                var mapper = JsonMapper.builder().build();
                var rows = mapper.createArrayNode();
                for (var transaction : json.path("data")) {
                    var row = rows.addObject();
                    for (String field : new String[]{"id", "transaction_date", "account_number", "va", "transfer_type", "amount_in", "amount_out", "transaction_content", "reference_number", "code", "bank_brand_name", "webhook_success"}) {
                        if (transaction.has(field)) row.set(field, transaction.get(field));
                    }
                }
                return rows.toPrettyString();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tra cứu bị gián đoạn; không có Payment nào được cập nhật.");
        } catch (java.io.IOException | RuntimeException ex) {
            throw new IllegalStateException("Không đọc được API SePay. Kiểm tra cấu hình/quyền/quota trên Dashboard; không có Payment nào được cập nhật.");
        }
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
