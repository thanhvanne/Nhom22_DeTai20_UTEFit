package com.utefit.controller.chat;

import com.utefit.service.ChatViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatViewController {
    private final ChatViewService chats;

    @GetMapping("/member/chat/trainer/{trainerId}")
    public String memberChatWithTrainer(@PathVariable Long trainerId,
                                        Principal principal,Model model) {
        put(model,chats.memberWithTrainer(principal.getName(),trainerId));
        return "chat/conversation";
    }
    @GetMapping("/pt/chat/member/{memberId}")
    public String ptChatWithMember(@PathVariable Long memberId,
                                   Principal principal,Model model) {
        put(model,chats.ptWithMember(principal.getName(),memberId));
        return "chat/conversation";
    }
    private void put(Model model, ChatViewService.ChatView c) {
        model.addAttribute("conversationId",c.conversationId());
        model.addAttribute("messages",c.messages());
        model.addAttribute("partnerName",c.partnerName());
        model.addAttribute("partnerAvatarUrl",c.partnerAvatarUrl());
        model.addAttribute("partnerIsTrainer",c.partnerIsTrainer());
        model.addAttribute("conversations",c.conversations());
    }
}
