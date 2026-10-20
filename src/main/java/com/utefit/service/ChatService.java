package com.utefit.service;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.dto.chat.ConversationSummaryResponse;

import java.util.List;

public interface ChatService {

    List<ConversationSummaryResponse> getConversationsForUser(Long userId);

    List<ChatMessageResponse> getRecentMessages(Long userId,
                                                Long conversationId,
                                                int limit);

    ChatMessageResponse sendMessage(Long senderUserId,
                                    Long conversationId,
                                    String content);

    Long getOrCreateConversation(Long memberUserId, Long trainerId);

    long countUnreadMessagesForUser(Long userId);
}