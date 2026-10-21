package com.utefit.controller.pt;

import com.utefit.dto.chat.ConversationSummaryResponse;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import com.utefit.service.ChatService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/pt")
@RequiredArgsConstructor
public class PtChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @GetMapping("/chats")
    public String listChats(Model model, Principal principal) {
        if (principal == null) {
            throw new EntityNotFoundException("Không tìm thấy người dùng đăng nhập.");
        }
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy người dùng với email: " + email));

        Long userId = user.getId();
        List<ConversationSummaryResponse> conversations =
                chatService.getConversationsForUser(userId);

        model.addAttribute("conversations", conversations);
        return "pt/chat/list";
    }
}