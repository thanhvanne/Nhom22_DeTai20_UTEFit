package com.utefit.validation;

import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class InputRules {
    public record Rule(String use, String type, Integer minLength, Integer maxLength, Integer maxBytes,
                       String pattern, String normalize, String min, String max, Integer scale,
                       Boolean required, String message) {}
    public record Scope(String path, String method, Map<String, Rule> fields) {}
    public record Contract(Map<String, Rule> fields, List<Scope> scopes) {}
    public static final Contract CONTRACT = load();

    private InputRules() {}

    private static Contract load() {
        try (var input = new ClassPathResource("static/js/validation-rules.json").getInputStream()) {
            return JsonMapper.builder().build().readValue(input, Contract.class);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Rule merge(Rule base, Rule override) {
        if (base == null) return override;
        if (override == null) return base;
        return new Rule(null, pick(override.type(), base.type()), pick(override.minLength(), base.minLength()),
                pick(override.maxLength(), base.maxLength()), pick(override.maxBytes(), base.maxBytes()),
                pick(override.pattern(), base.pattern()), pick(override.normalize(), base.normalize()),
                pick(override.min(), base.min()), pick(override.max(), base.max()), pick(override.scale(), base.scale()),
                pick(override.required(), base.required()), pick(override.message(), base.message()));
    }

    private static <Value> Value pick(Value preferred, Value fallback) {
        return preferred == null ? fallback : preferred;
    }

    private static Rule resolve(Rule rule) {
        return rule != null && rule.use() != null ? merge(rule(rule.use()), rule) : rule;
    }

    public static Rule rule(String field) {
        Rule rule = CONTRACT.fields().get(field);
        if (rule == null && (field.endsWith("Id") || field.equals("branchIds") || field.equals("beforeId"))) {
            rule = CONTRACT.fields().get("id");
        }
        return resolve(rule);
    }

    public static Rule rule(String path, String method, String field) {
        Rule result = rule(field);
        for (Scope scope : CONTRACT.scopes()) {
            if (path.matches(scope.path()) && (scope.method() == null || scope.method().equalsIgnoreCase(method))) {
                Rule override = scope.fields().get(field);
                if (override != null) result = merge(result, resolve(override));
            }
        }
        return result;
    }

    public static Map<String, Rule> required(String path, String method) {
        Map<String, Rule> result = new LinkedHashMap<>();
        for (Scope scope : CONTRACT.scopes()) {
            if (path.matches(scope.path()) && (scope.method() == null || scope.method().equalsIgnoreCase(method))) {
                scope.fields().keySet().forEach(field -> {
                    Rule rule = rule(path, method, field);
                    if (Boolean.TRUE.equals(rule.required())) result.put(field, rule);
                });
            }
        }
        return result;
    }

    public static String normalize(String value, Rule rule) {
        if (value == null || rule == null || rule.normalize() == null) return value;
        String stripped = value.replaceAll("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$", "");
        return switch (rule.normalize()) {
            case "email" -> stripped.toLowerCase(Locale.ROOT);
            case "name" -> Normalizer.normalize(stripped.replaceAll("[\\s\\p{Z}\\uFEFF]+", " "), Normalizer.Form.NFC);
            case "mobile" -> stripped.startsWith("+84") ? "0" + stripped.substring(3) : stripped;
            case "hotline" -> stripped.replaceAll("[ ().-]", "");
            default -> stripped;
        };
    }

    public static String error(String field, String value) {
        return error(rule(field), value);
    }

    public static String error(Rule rule, String value) {
        if (rule == null) return null;
        String normalized = normalize(value, rule);
        if (normalized == null || normalized.isBlank()) {
            return Boolean.TRUE.equals(rule.required()) ? "Vui lòng nhập hoặc chọn giá trị cho trường này." : null;
        }
        boolean invalid = normalized.matches("(?s).*[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f].*");
        invalid |= rule.minLength() != null && normalized.length() < rule.minLength();
        invalid |= rule.maxLength() != null && normalized.length() > rule.maxLength();
        invalid |= rule.maxBytes() != null && normalized.getBytes(StandardCharsets.UTF_8).length > rule.maxBytes();
        invalid |= rule.pattern() != null && !Pattern.compile(rule.pattern()).matcher(normalized).matches();
        invalid |= "email".equals(rule.normalize()) && normalized.indexOf('@') > 64;
        if ("integer".equals(rule.type()) || "decimal".equals(rule.type())) {
            try {
                if (normalized.length() > 32) return rule.message();
                if (!normalized.matches("integer".equals(rule.type()) ? "[0-9]+" : "[0-9]+(?:\\.[0-9]+)?")) {
                    invalid = true;
                } else {
                    BigDecimal number = new BigDecimal(normalized);
                    invalid |= rule.min() != null && number.compareTo(new BigDecimal(rule.min())) < 0;
                    invalid |= rule.max() != null && number.compareTo(new BigDecimal(rule.max())) > 0;
                    invalid |= rule.scale() != null && number.stripTrailingZeros().scale() > rule.scale();
                }
            } catch (NumberFormatException exception) {
                invalid = true;
            }
        }
        return invalid ? (rule.message() == null ? "Giá trị không hợp lệ." : rule.message()) : null;
    }

    public static void require(String field, String value) {
        String message = error(field, value);
        if (value == null || value.isBlank()) message = "Vui lòng nhập giá trị cho trường này.";
        if (message != null) throw new InputViolationException(Map.of(field, message));
    }

    public static void check(String field, String value) {
        String message = error(field,value);
        if (message != null) throw new InputViolationException(Map.of(field,message));
    }
}
