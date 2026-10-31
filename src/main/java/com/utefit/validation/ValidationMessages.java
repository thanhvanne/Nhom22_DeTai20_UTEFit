package com.utefit.validation;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ValidationMessages {
    private ValidationMessages() {}

    public static Map<String,String> fields(String message, Set<String> available) {
        if (message == null) return Map.of();
        String lower = message.toLowerCase(Locale.ROOT);
        String[] candidates;
        if (lower.contains("email")) candidates = new String[]{"email"};
        else if (lower.contains("xác nhận mật khẩu") || lower.contains("mật khẩu xác nhận")) candidates = new String[]{"confirmPassword"};
        else if (lower.contains("mật khẩu hiện tại")) candidates = new String[]{"currentPassword"};
        else if (lower.contains("mật khẩu")) candidates = new String[]{"newPassword","password"};
        else if (lower.contains("otp")) candidates = new String[]{"code"};
        else if (lower.contains("tham chiếu") || lower.contains("mã ngân hàng") || lower.contains("mã giao dịch")) candidates = new String[]{"bankReference","paymentReference"};
        else if (lower.contains("điện thoại")) candidates = new String[]{"phone"};
        else if (lower.contains("video")) candidates = new String[]{"video"};
        else if (lower.contains("ảnh")) candidates = new String[]{"avatarFile","avatar","imageFile","image"};
        else if (lower.contains("khuyến mãi") || lower.contains("ưu đãi") || lower.contains("mã giảm")) candidates = new String[]{"promotionCode","code","offerSelection"};
        else if (lower.contains("chuyên môn")) candidates = new String[]{"specialization"};
        else if (lower.contains("kinh nghiệm")) candidates = new String[]{"yearsExperience"};
        else if (lower.contains("thời hạn") || lower.contains("thời gian thưởng")) candidates = new String[]{"durationQuantity","bonusQuantity"};
        else if (lower.contains("đóng cửa")) candidates = new String[]{"endTime","closingTime"};
        else if (lower.contains("mở cửa")) candidates = new String[]{"startTime","openingTime"};
        else if (lower.contains("kết thúc")) candidates = new String[]{"endTime","endDate"};
        else if (lower.contains("ngày") || lower.contains("lịch") || lower.contains("xung đột")) candidates = new String[]{"scheduleDate","startTime","startDate","endDate"};
        else if (lower.contains("chi nhánh")) candidates = new String[]{"branchId","branchIds"};
        else if (lower.contains("huấn luyện viên") || lower.contains("pt ")) candidates = new String[]{"trainerId"};
        else if (lower.contains("giá") || lower.contains("tiền")) candidates = new String[]{"price","amount"};
        else if (lower.contains("sức chứa")) candidates = new String[]{"capacity"};
        else if (lower.contains("tên")) candidates = new String[]{"name","firstName","lastName"};
        else if (lower.contains("lớp")) candidates = new String[]{"fitnessClassId","categoryId"};
        else if (lower.contains("qr")) candidates = new String[]{"qrToken"};
        else if (lower.contains("tin nhắn")) candidates = new String[]{"content"};
        else return Map.of();
        for (String field : candidates) if (available.contains(field)) return Map.of(field,message);
        return Map.of();
    }
}
