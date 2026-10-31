package com.utefit.websocket;

import com.utefit.dto.chat.ChatMessageSendRequest;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import com.utefit.service.ChatService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * Client gửi STOMP message tới /app/chat.send
     * payload: { conversationId, content }
     */
    @MessageMapping("/chat.send")
    public void handleSend(@jakarta.validation.Valid ChatMessageSendRequest request, Principal principal) {
        if (principal == null) {
            return; // chưa đăng nhập -> bỏ qua
        }
        com.utefit.validation.FormChecks.validate(request);

        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy tài khoản đăng nhập."));

        chatService.sendMessage(user.getId(), request.getConversationId(), request.getContent());
        // Realtime push đã được ChatServiceImpl xử lý qua SimpMessagingTemplate
    }
}
