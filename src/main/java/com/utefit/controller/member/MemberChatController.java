package com.utefit.controller.member;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.service.MemberChatService;
import com.utefit.service.MemberTrainerRequestService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/member/chats")
@RequiredArgsConstructor
public class MemberChatController {

    private final MemberChatService chats;
    private final MemberTrainerRequestService requests;

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute(
                "conversations",
                chats.getMyConversations(principal.getName())
        );

        model.addAttribute(
                "acceptedTrainers",
                requests.getAcceptedTrainers(principal.getName())
        );

        return "member/chats";
    }

    @PostMapping("/start/{trainerId}")
    public String start(
            @PathVariable Long trainerId,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            return "redirect:/member/chats/"
                    + chats.startConversation(
                    principal.getName(),
                    trainerId
            );

        } catch (IllegalArgumentException
                 | IllegalStateException
                 | EntityNotFoundException ex) {

            redirect.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/member/chats";
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {
        String email = principal.getName();

        model.addAttribute(
                "chat",
                chats.openConversation(email, id)
        );

        // Sidebar: chỉ lấy hội thoại thuộc Member này.
        model.addAttribute(
                "conversations",
                chats.getMyConversations(email)
        );

        model.addAttribute(
                "acceptedTrainers",
                requests.getAcceptedTrainers(email)
        );

        return "member/chat-conversation";
    }

    @GetMapping("/{id}/messages")
    @ResponseBody
    public List<ChatMessageResponse> messages(
            @PathVariable Long id,
            Principal principal
    ) {
        return chats.getMessages(
                principal.getName(),
                id
        );
    }

    @PostMapping("/{id}/send")
    @ResponseBody
    public ChatMessageResponse send(
            @PathVariable Long id,
            @RequestParam String content,
            Principal principal
    ) {
        try {
            return chats.sendMessage(
                    principal.getName(),
                    id,
                    content
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage()
            );
        }
    }
}