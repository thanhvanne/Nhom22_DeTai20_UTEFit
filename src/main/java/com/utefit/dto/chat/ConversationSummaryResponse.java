package com.utefit.dto.chat;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummaryResponse {

    private Long conversationId;

    private Long partnerUserId;
    private String partnerName;
    private String partnerAvatarUrl;
    private boolean partnerIsTrainer; 

    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;

    private long unreadCount;
}