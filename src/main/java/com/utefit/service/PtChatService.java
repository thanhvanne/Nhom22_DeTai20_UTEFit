package com.utefit.service;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.dto.chat.ConversationSummaryResponse;
import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Nghiệp vụ chat dành cho PT.
 * Controller -> PtChatService -> ChatService/Repository.
 */
@Service
@RequiredArgsConstructor
public class PtChatService {

    private final UserRepository userRepository;
    private final ChatService chatService;

    public List<ConversationSummaryResponse> getConversations(String email) {
        Long ptId = requirePtId(email);
        return chatService.getConversationsForUser(ptId)
                .stream()
                .filter(c -> !c.isPartnerIsTrainer())
                .toList();
    }

    public List<ChatMessageResponse> getMessages(String email, Long conversationId) {
        Long ptId = requirePtId(email);
        // ChatService kiểm tra userId đúng là người tham gia hội thoại.
        return chatService.getRecentMessages(ptId, conversationId, 50);
    }

    public ChatMessageResponse sendMessage(String email, Long conversationId, String content) {
        Long ptId = requirePtId(email);
        // ChatService kiểm tra tư cách tham gia và TrainerRequest ACCEPTED.
        return chatService.sendMessage(ptId, conversationId, content);
    }

    private Long requirePtId(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Bạn cần đăng nhập tài khoản PT.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản PT."));
        if (user.getRole() != UserRole.PT) {
            throw new AccessDeniedException("Tài khoản không có quyền PT.");
        }
        return user.getId();
    }
}