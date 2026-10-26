package com.utefit.util;

import org.springframework.stereotype.Component;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component("membershipDisplay")
public class MembershipDisplay {
    private static final Pattern PLAN = Pattern.compile("^DEMO B[1-5] - (.+)$");
    private static final Pattern BRANCH = Pattern.compile("^UTEFit Demo Branch ([1-5])$");
    private static final Map<String, String> TERMS = Map.ofEntries(
            Map.entry("1 day", "1 ngày"), Map.entry("7 days", "7 ngày"),
            Map.entry("2 weeks", "2 tuần"), Map.entry("1 month", "1 tháng"),
            Map.entry("3 months", "3 tháng"), Map.entry("6 months", "6 tháng"),
            Map.entry("1 year", "1 năm"), Map.entry("2 years", "2 năm"),
            Map.entry("3 months + 2 weeks bonus", "3 tháng - Tặng thêm 2 tuần"),
            Map.entry("6 months + 1 month bonus", "6 tháng - Tặng thêm 1 tháng"),
            Map.entry("1 year + 2 months bonus", "1 năm - Tặng thêm 2 tháng"));

    public String name(String value) {
        if (value == null) return "";
        var branch = BRANCH.matcher(value);
        if (branch.matches()) return "UTEFit - Chi nhánh " + branch.group(1);
        var plan = PLAN.matcher(value);
        if (plan.matches() && TERMS.containsKey(plan.group(1))) return "Gói tập " + TERMS.get(plan.group(1));
        return value;
    }

    public String benefit(Object value) {
        if (value == null) return "";
        return switch (value.toString()) {
            case "GYM" -> "Phòng gym";
            case "YOGA" -> "Yoga";
            case "PT" -> "Huấn luyện viên cá nhân";
            case "ALL" -> "Tất cả dịch vụ";
            default -> value.toString();
        };
    }

    public String money(Number amount) {
        if (amount == null) return "";
        var formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount) + "đ";
    }

    public String paymentStatus(Object value) {
        if (value == null) return "";
        try { return com.utefit.enums.PaymentStatus.valueOf(value.toString()).getLabel(); }
        catch (IllegalArgumentException ex) { return value.toString(); }
    }
}
