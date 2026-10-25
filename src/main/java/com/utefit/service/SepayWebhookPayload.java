package com.utefit.service;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HexFormat;
import java.util.Locale;

public record SepayWebhookPayload(long id, String gateway, String accountNumber, String code, String content,
                                  String transferType, BigDecimal amount, String referenceCode,
                                  LocalDateTime transactionDate, String fingerprint, String bodyHash) {
    private static final JsonMapper JSON = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    public static SepayWebhookPayload parse(byte[] body) {
        try {
            JsonNode root = JSON.readTree(body);
            if (!root.isObject()) throw new IllegalArgumentException("Expected JSON object");
            JsonNode idNode = root.get("id");
            if (idNode == null || !idNode.isIntegralNumber() || !idNode.canConvertToLong() || idNode.longValue() <= 0) {
                throw new IllegalArgumentException("Invalid transaction id");
            }
            JsonNode amountNode = root.get("transferAmount");
            if (amountNode == null || !amountNode.isIntegralNumber()) throw new IllegalArgumentException("Expected integer VND amount");
            BigDecimal amount = amountNode.decimalValue();
            if (amount.signum() <= 0 || amount.compareTo(new BigDecimal("9999999999999")) > 0) throw new IllegalArgumentException("Invalid amount");
            long id = idNode.longValue();
            String gateway = text(root, "gateway", 100, true).toUpperCase(Locale.ROOT);
            String account = text(root, "accountNumber", 50, true);
            String code = text(root, "code", 100, false).toUpperCase(Locale.ROOT);
            String content = text(root, "content", 2000, true);
            String type = text(root, "transferType", 3, true);
            if (!type.equals("in") && !type.equals("out")) throw new IllegalArgumentException("Invalid transfer type");
            String reference = text(root, "referenceCode", 100, false);
            LocalDateTime date = LocalDateTime.parse(text(root, "transactionDate", 19, true), DATE);
            String fingerprint = hash(Long.toString(id), gateway, account, code, content, type, amount.toBigIntegerExact().toString(), reference, DATE.format(date));
            return new SepayWebhookPayload(id, gateway, account, code, content, type, amount, reference, date, fingerprint,
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid SePay JSON payload", ex);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String text(JsonNode root, String field, int max, boolean required) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            if (required) throw new IllegalArgumentException("Missing field");
            return "";
        }
        if (!node.isString()) throw new IllegalArgumentException("Expected string field");
        String value = node.asString().trim();
        if (value.length() > max || (required && value.isBlank()) || value.chars().anyMatch(character -> character < 32)) {
            throw new IllegalArgumentException("Invalid string field");
        }
        return value;
    }

    private static String hash(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                byte[] encoded = field.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(encoded.length).array());
                digest.update(encoded);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
