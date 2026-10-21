package com.utefit.service;

import com.utefit.dto.notification.NotificationDto;
import com.utefit.dto.notification.NotificationResponse;
import com.utefit.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    // Đã được dùng trong ChatServiceImpl
    void createNotificationForUser(Long userId,
                                   NotificationType type,
                                   String title,
                                   String content,
                                   Long referenceId);

    // Dùng cho trang /notifications (Member/PT)
    List<NotificationDto> getNotificationsForUser(Long userId);

    long countUnreadForUser(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    // Giữ lại cho MemberNotificationService (A đã viết)
    List<NotificationResponse> getRecentNotifications(Long userId, int limit);

    long countUnreadNotifications(Long userId);
}