package com.utefit.enums;

public enum PaymentStatus {

    PENDING,
    CANCEL_REQUESTED,
    CANCELLED,
    PAID,
    FAILED,
    REFUNDED;

    public String getLabel() {
        return switch (this) {
            case PENDING -> "Chờ thanh toán";
            case CANCEL_REQUESTED -> "Đang yêu cầu hủy";
            case CANCELLED -> "Đã hủy đơn chờ";
            case PAID -> "Đã thanh toán";
            case FAILED -> "Không thành công";
            case REFUNDED -> "Đã hoàn tiền";
        };
    }
}
