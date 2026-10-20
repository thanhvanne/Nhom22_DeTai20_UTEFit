package com.utefit.dto.chat;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long conversationId;

    private Long senderId;
    private String senderName;
    private String senderAvatarUrl;

    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    
    private boolean mine;
}