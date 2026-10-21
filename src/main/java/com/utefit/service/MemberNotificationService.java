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
        return notificationService.getRecentNotifications(
                accounts.requireMemberId(email), 20);
    }

    public long countUnread(String email) {
        return notificationService.countUnreadNotifications(
                accounts.requireMemberId(email));
    }

    public void markRead(String email, Long notificationId) {
        notificationService.markAsRead(accounts.requireMemberId(email), notificationId);
    }

    public void markAllRead(String email) {
        notificationService.markAllAsRead(accounts.requireMemberId(email));
    }
}