package com.utefit.service;

import com.utefit.dto.notification.NotificationResponse;
import com.utefit.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotificationForUser(
            Long userId,
            NotificationType type,
            String title,
            String content,
            Long relatedId
    );

    List<NotificationResponse> getRecentNotifications(Long userId, int limit);

    long countUnreadNotifications(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}