package com.utefit.controller.pt;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.service.PtChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/pt")
@RequiredArgsConstructor
public class PtChatController {

    private final PtChatService ptChats;

    @GetMapping("/chats")
    public String listChats(Model model, Principal principal) {
        var conversations = ptChats.getConversations(email(principal));

        // Mở trực tiếp cuộc trò chuyện gần nhất,
        // không qua trang danh sách trung gian.
        if (!conversations.isEmpty()) {
            return "redirect:/pt/chat/member/"
                    + conversations.get(0).getPartnerUserId();
        }

        // Chưa có hội thoại: hiển thị giao diện 2 cột.
        model.addAttribute("conversations", conversations);
        return "pt/chat/list";
    }

    /** REST: tải lịch sử chat mà không reload cả trang. */
    @GetMapping("/chats/{id}/messages")
    @ResponseBody
    public List<ChatMessageResponse> messages(
            @PathVariable Long id,
            Principal principal
    ) {
        try {
            return ptChats.getMessages(email(principal), id);
        } catch (IllegalStateException | AccessDeniedException ex) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    ex.getMessage(),
                    ex
            );
        }
    }

    /** REST: gửi tin qua HTTP POST, lưu database. */
    @PostMapping("/chats/{id}/send")
    @ResponseBody
    public ChatMessageResponse send(
            @PathVariable Long id,
            @RequestParam("content") String content,
            Principal principal
    ) {
        try {
            return ptChats.sendMessage(
                    email(principal),
                    id,
                    content
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage(),
                    ex
            );
        } catch (IllegalStateException | AccessDeniedException ex) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    ex.getMessage(),
                    ex
            );
        }
    }

    private String email(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập."
            );
        }
        return principal.getName();
    }
}