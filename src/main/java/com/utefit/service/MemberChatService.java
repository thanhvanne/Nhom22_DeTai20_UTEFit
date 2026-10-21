package com.utefit.service;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.dto.chat.ConversationSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberChatService {

    private final MemberAccountService accounts;
    private final ChatService chatService; // B sở hữu

    public List<ConversationSummaryResponse> getMyConversations(String email) {
        Long memberId = accounts.requireMemberId(email);
        return chatService.getConversationsForUser(memberId).stream()
                .filter(ConversationSummaryResponse::isPartnerIsTrainer)
                .toList();
    }

    public Long startConversation(String email, Long trainerId) {
        if (trainerId == null || trainerId <= 0) {
            throw new IllegalArgumentException("PT không hợp lệ.");
        }
        // B xác minh TrainerRequest đã ACCEPTED và cặp Member–PT hợp lệ.
        return chatService.getOrCreateConversation(
                accounts.requireMemberId(email), trainerId);
    }

    public ChatPage openConversation(String email, Long conversationId) {
        Long memberId = accounts.requireMemberId(email);
        ConversationSummaryResponse conversation = ownedConversation(memberId, conversationId);
        return new ChatPage(conversationId, memberId,
                conversation.getPartnerName(),
                chatService.getRecentMessages(memberId, conversationId, 50));
    }

    public List<ChatMessageResponse> getMessages(String email, Long conversationId) {
        Long memberId = accounts.requireMemberId(email);
        ownedConversation(memberId, conversationId);
        return chatService.getRecentMessages(memberId, conversationId, 50);
    }

    public ChatMessageResponse sendMessage(String email, Long conversationId, String content) {
        Long memberId = accounts.requireMemberId(email);
        ownedConversation(memberId, conversationId);
        String clean = content == null ? "" : content.trim();
        if (clean.isBlank() || clean.length() > 2000) {
            throw new IllegalArgumentException("Tin nhắn phải từ 1 đến 2000 ký tự.");
        }
        // B lưu Message, tạo Notification và đẩy STOMP realtime.
        return chatService.sendMessage(memberId, conversationId, clean);
    }

    private ConversationSummaryResponse ownedConversation(Long memberId, Long id) {
        if (id == null || id <= 0) {
            throw new AccessDeniedException("Conversation không hợp lệ.");
        }
        return chatService.getConversationsForUser(memberId).stream()
                .filter(c -> id.equals(c.getConversationId()) && c.isPartnerIsTrainer())
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "Bạn không có quyền truy cập cuộc trò chuyện này."));
    }

    public record ChatPage(Long conversationId, Long currentUserId,
                           String partnerName, List<ChatMessageResponse> messages) {}
}