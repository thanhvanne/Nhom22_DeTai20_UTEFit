package com.utefit.controller.chat;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatHistoryController {
    private final UserRepository users;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    @GetMapping({"/member/chats/{id}/older", "/pt/chats/{id}/older"})
    @Transactional
    public List<ChatMessageResponse> older(@PathVariable Long id, @RequestParam Long beforeId, Principal principal) {
        if (beforeId == null || beforeId <= 0) throw new IllegalArgumentException("Mốc tin nhắn không hợp lệ.");
        var user = users.findByEmail(principal.getName()).orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
        var c = conversations.findById(id).orElseThrow(() -> new AccessDeniedException("Không có hội thoại."));
        if (!c.getMember().getId().equals(user.getId()) && !c.getTrainer().getUser().getId().equals(user.getId()))
            throw new AccessDeniedException("Không có quyền đọc hội thoại.");
        return messages.findByConversation_IdAndIdLessThanOrderByIdDesc(id, beforeId, PageRequest.of(0, 50)).stream()
                .peek(m -> { if (!m.getSender().getId().equals(user.getId()) && m.getReadAt() == null) m.setReadAt(java.time.LocalDateTime.now()); })
                .map(m -> ChatMessageResponse.builder().id(m.getId()).conversationId(id).senderId(m.getSender().getId())
                        .senderName(m.getSender().getFullName()).content(m.getContent()).sentAt(m.getSentAt())
                        .readAt(m.getReadAt()).mine(m.getSender().getId().equals(user.getId())).build()).toList();
    }
}
