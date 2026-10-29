package com.utefit.service;

import com.utefit.dto.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberNotificationService {

    private final MemberAccountService accounts;
    @org.springframework.beans.factory.annotation.Autowired private com.utefit.repository.NotificationRepository repository;
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<NotificationResponse> page(String email, int page) {
        return repository.findByUser_IdOrderByCreatedAtDescIdDesc(accounts.requireMemberId(email), org.springframework.data.domain.PageRequest.of(Math.max(0, Math.min(10000, page)), 20))
            .map(n -> NotificationResponse.builder().id(n.getId()).userId(n.getUser().getId()).type(n.getType()).title(n.getTitle()).content(n.getContent()).relatedId(n.getRelatedId()).read(Boolean.TRUE.equals(n.getRead())).createdAt(n.getCreatedAt()).readAt(n.getReadAt()).build());
    }
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