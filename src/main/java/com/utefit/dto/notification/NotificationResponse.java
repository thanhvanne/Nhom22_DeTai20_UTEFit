package com.utefit.dto.notification;

import com.utefit.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;

    private Long userId;

    private NotificationType type;

    private String title;

    private String content;

    private Long relatedId;

    private boolean read;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}