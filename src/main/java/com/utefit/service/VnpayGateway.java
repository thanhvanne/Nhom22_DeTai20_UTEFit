package com.utefit.service;

import com.utefit.config.VnpayProperties;
import com.utefit.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VnpayGateway {
    public static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final VnpayProperties properties;

    public String paymentUrl(Payment payment, String ipAddress) {
        properties.requireConfigured();
        LocalDateTime created = payment.getCreatedAt();
        if (!created.plusMinutes(15).isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Yêu cầu VNPay đã hết thời gian thanh toán. Vui lòng chờ đối soát giao dịch trước khi tạo lại.");
        }
        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_Version", "2.1.0");
        fields.put("vnp_Command", "pay");
        fields.put("vnp_TmnCode", properties.getTmnCode());
        fields.put("vnp_Amount", minorUnits(payment.getAmount()));
        fields.put("vnp_CurrCode", "VND");
        fields.put("vnp_TxnRef", payment.getTransactionCode());
        fields.put("vnp_OrderInfo", "Thanh toan goi UTEFit " + payment.getId());
        fields.put("vnp_OrderType", "other");
        fields.put("vnp_Locale", "vn");
        fields.put("vnp_ReturnUrl", properties.getReturnUrl());
        fields.put("vnp_IpAddr", ipAddress);
        fields.put("vnp_CreateDate", TIMESTAMP.format(created));
        fields.put("vnp_ExpireDate", TIMESTAMP.format(created.plusMinutes(15)));
        return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"
                + canonical(fields) + "&vnp_SecureHash=" + signature(fields);
    }

    public boolean verified(Map<String, String> fields) {
        if (!properties.isEnabled() || properties.getHashSecret() == null
                || properties.getHashSecret().isBlank()
                || !properties.getTmnCode().equals(fields.get("vnp_TmnCode"))) {
            return false;
        }
        String supplied = fields.get("vnp_SecureHash");
        if (supplied == null || !supplied.matches("(?i)[0-9a-f]{128}")) return false;
        return MessageDigest.isEqual(HexFormat.of().parseHex(signature(fields)),
                HexFormat.of().parseHex(supplied));
    }

    public String signature(Map<String, String> fields) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(canonical(fields).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Không thể ký yêu cầu thanh toán.", ex);
        }
    }

    public static String minorUnits(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Số tiền không hợp lệ.");
        return amount.movePointRight(2).toBigIntegerExact().toString();
    }

    private String canonical(Map<String, String> fields) {
        return new TreeMap<>(fields).entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("vnp_")
                        && !entry.getKey().equals("vnp_SecureHash")
                        && !entry.getKey().equals("vnp_SecureHashType")
                        && entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
