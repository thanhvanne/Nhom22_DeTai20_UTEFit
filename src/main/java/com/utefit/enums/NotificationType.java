package com.utefit.enums;

public enum NotificationType {
    TRAINER_REQUEST_SENT,          // Member gửi yêu cầu PT
    TRAINER_REQUEST_ACCEPTED,      // PT chấp nhận yêu cầu
    TRAINER_REQUEST_REJECTED,      // PT từ chối
    PERSONAL_SCHEDULE_CREATED,     // PT tạo lịch PT cá nhân
    CLASS_SCHEDULE_CANCELLED,      // Admin huỷ lớp/lịch (C sẽ dùng)
    MESSAGE_RECEIVED,              // Tin nhắn mới trong chat
    MEMBERSHIP_ACTIVATED,          // Admin xác nhận CASH
    PERSONAL_SCHEDULE_CANCELLED    // PT hủy buổi cá nhân
}
