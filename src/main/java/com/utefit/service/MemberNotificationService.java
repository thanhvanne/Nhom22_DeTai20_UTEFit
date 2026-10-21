package com.utefit.service;

import com.utefit.dto.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberNotificationService {

    private final MemberAccountService accounts;
    private final NotificationService notificationService; // B sở hữu

    public List<NotificationResponse> getMyNotifications(String email) {
        Long userId = accounts.requireMemberId(email);
        return notificationService.getRecentNotifications(userId, 20);
    }

    public long countUnread(String email) {
        Long userId = accounts.requireMemberId(email);
        return notificationService.countUnreadNotifications(userId);
    }

    public void markRead(String email, Long notificationId) {
        Long userId = accounts.requireMemberId(email);
        notificationService.markAsRead(notificationId, userId);
    }

    public void markAllRead(String email) {
        Long userId = accounts.requireMemberId(email);
        notificationService.markAllAsRead(userId);
    }
}