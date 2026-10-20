package com.utefit.service.impl;

import com.utefit.dto.notification.NotificationResponse;
import com.utefit.entity.Notification;
import com.utefit.entity.User;
import com.utefit.enums.NotificationType;
import com.utefit.repository.NotificationRepository;
import com.utefit.repository.UserRepository;
import com.utefit.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationResponse createNotificationForUser(
            Long userId,
            NotificationType type,
            String title,
            String content,
            Long relatedId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy người nhận thông báo."));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        NotificationResponse response = toResponse(saved);

        // Gửi realtime qua STOMP (nếu client đang connect WebSocket)
        try {
            // principal name = email → dùng email làm "user"
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/notifications",
                    response
            );
        } catch (Exception ignored) {
            // Không để lỗi WebSocket làm hỏng luồng lưu DB
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(Long userId, int limit) {
        List<Notification> list = notificationRepository
                .findTop20ByUser_IdOrderByCreatedAtDesc(userId);

        if (limit > 0 && limit < list.size()) {
            list = list.subList(0, limit);
        }

        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countByUser_IdAndReadIsFalse(userId);
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy thông báo."));

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        List<Notification> unreadList = notificationRepository
                .findByUser_IdAndReadIsFalseOrderByCreatedAtDesc(userId);

        LocalDateTime now = LocalDateTime.now();

        for (Notification n : unreadList) {
            n.setRead(true);
            n.setReadAt(now);
        }
    }

    private NotificationResponse toResponse(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .relatedId(entity.getRelatedId())
                .read(Boolean.TRUE.equals(entity.getRead()))
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }
}