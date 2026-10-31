package com.utefit.validation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RequestChecks {
    private static final Set<String> DATES = Set.of("startDate", "endDate", "scheduleDate", "date", "weekStart", "from", "to", "fromDate", "toDate");
    private static final Set<String> TIMES = Set.of("openingTime", "closingTime", "startTime", "endTime");

    private RequestChecks() {}

    public static Map<String, String> errors(String path, String method, Map<String, String[]> values) {
        Map<String, String> errors = new LinkedHashMap<>();
        values.forEach((field, entries) -> {
            if (field.startsWith("_") || field.equals("password") && method.equals("GET")) return;
            for (String entry : entries) {
                String error = InputRules.error(InputRules.rule(path, method, field), entry);
                if (error != null) errors.putIfAbsent(field, error);
                if (entry != null && !entry.isBlank()) {
                    try {
                        if (DATES.contains(field)) {
                            LocalDate date = LocalDate.parse(entry);
                            if (date.getYear() < 1 || date.getYear() > 9999) throw new DateTimeParseException("date",entry,0);
                        }
                        if (TIMES.contains(field)) LocalTime.parse(entry);
                        if (field.equals("bankDate")) {
                            LocalDateTime date = LocalDateTime.parse(entry);
                            if (date.getYear() < 1 || date.getYear() > 9999) throw new DateTimeParseException("date",entry,0);
                        }
                    } catch (DateTimeParseException exception) {
                        errors.putIfAbsent(field, TIMES.contains(field) ? "Giờ không hợp lệ. Vui lòng nhập theo dạng HH:mm." : "Ngày không hợp lệ. Vui lòng chọn lại ngày.");
                    }
                }
            }
        });
        InputRules.required(path, method).forEach((field, rule) -> {
            if (get(values,field).isBlank()) errors.putIfAbsent(field,"Vui lòng nhập hoặc chọn giá trị cho trường này.");
        });
        ordered(values, errors, "openingTime", "closingTime", false, "Giờ đóng cửa phải sau giờ mở cửa.");
        ordered(values, errors, "startTime", "endTime", false, "Giờ kết thúc phải sau giờ bắt đầu.");
        ordered(values, errors, "startDate", "endDate", !path.equals("/admin/promotions/save"), "Ngày kết thúc phải sau ngày bắt đầu.");
        ordered(values, errors, "from", "to", true, "Ngày kết thúc không được trước ngày bắt đầu.");
        ordered(values, errors, "fromDate", "toDate", true, "Ngày kết thúc không được trước ngày bắt đầu.");
        String password = get(values, values.containsKey("newPassword") ? "newPassword" : "password");
        if (values.containsKey("confirmPassword") && !password.equals(get(values,"confirmPassword"))) {
            errors.put("confirmPassword","Mật khẩu xác nhận không khớp.");
        }
        if (method.equals("POST") && !errors.containsKey("scheduleDate") && !errors.containsKey("startTime")
                && !get(values,"scheduleDate").isBlank() && !get(values,"startTime").isBlank()) {
            LocalDateTime start = LocalDate.parse(get(values,"scheduleDate")).atTime(LocalTime.parse(get(values,"startTime")));
            if (!start.isAfter(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))) {
                errors.put("scheduleDate","Buổi tập phải bắt đầu trong tương lai.");
            }
        }
        return errors;
    }

    private static void ordered(Map<String,String[]> values, Map<String,String> errors, String start, String end, boolean inclusive, String message) {
        String first = get(values,start);
        String last = get(values,end);
        if (first.isBlank() || last.isBlank() || errors.containsKey(start) || errors.containsKey(end)) return;
        if (last.compareTo(first) < 0 || !inclusive && last.equals(first)) errors.put(end,message);
    }

    public static String get(Map<String,String[]> values, String name) {
        String[] entries = values.get(name);
        return entries == null || entries.length == 0 || entries[0] == null ? "" : entries[0];
    }
}
