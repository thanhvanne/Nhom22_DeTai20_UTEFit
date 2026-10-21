package com.utefit.service.impl;

import com.utefit.dto.notification.NotificationDto;
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
    public void createNotificationForUser(Long userId,
                                          NotificationType type,
                                          String title,
                                          String content,
                                          Long referenceId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng."));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);

        // LƯU relatedId để client build URL khi click thông báo
        if (referenceId != null) {
            notification.setRelatedId(referenceId); // <-- entity Notification cần có field relatedId
        }

        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);

        // Đẩy realtime qua STOMP (principal = email, giống ChatServiceImpl)
        try {
            NotificationDto dto = mapToDto(saved);
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/notifications",
                    dto
            );
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadForUser(Long userId) {
        return notificationRepository.countByUser_IdAndReadIsFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification noti = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông báo."));

        if (!Boolean.TRUE.equals(noti.getRead())) {
            noti.setRead(true);
            noti.setReadAt(LocalDateTime.now());
            notificationRepository.save(noti);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        List<Notification> unread =
                notificationRepository.findByUser_IdAndReadIsFalseOrderByCreatedAtDesc(userId);
        if (unread.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unread);
    }

    // ================== DÙNG CHO MEMBER ==================

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(Long userId, int limit) {
        if (limit <= 0) {
            limit = 20;
        }
        List<Notification> list =
                notificationRepository.findTop20ByUser_IdOrderByCreatedAtDesc(userId);

        if (list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countByUser_IdAndReadIsFalse(userId);
    }

    // ================== MAPPERS ==================

    private NotificationDto mapToDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setContent(n.getContent());
        dto.setType(n.getType() != null ? n.getType().name() : null);
        dto.setRead(Boolean.TRUE.equals(n.getRead()));
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUser() != null ? n.getUser().getId() : null)
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                // map entity.relatedId -> DTO.relatedId
                .relatedId(n.getRelatedId())
                .read(Boolean.TRUE.equals(n.getRead()))
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}