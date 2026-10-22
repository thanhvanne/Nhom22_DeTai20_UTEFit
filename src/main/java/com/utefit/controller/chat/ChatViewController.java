package com.utefit.controller.chat;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.entity.Trainer;
import com.utefit.entity.User;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.UserRepository;
import com.utefit.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@Controller
public class ChatViewController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;

    public ChatViewController(
            ChatService chatService,
            UserRepository userRepository,
            TrainerRepository trainerRepository
    ) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.trainerRepository = trainerRepository;
    }

    /**
     * URL chat Member cũ.
     * Giữ nguyên đường dẫn và chuyển sang bố cục 2 cột.
     */
    @GetMapping("/member/chat/trainer/{trainerId}")
    public String memberChatWithTrainer(
            @PathVariable("trainerId") Long trainerId,
            Principal principal,
            Model model
    ) {

        User member = userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Không xác định được hội viên"
                        )
                );

        Trainer trainer = trainerRepository
                .findById(trainerId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy HLV"
                        )
                );

        Long conversationId;

        try {
            conversationId =
                    chatService.getOrCreateConversation(
                            member.getId(),
                            trainer.getId()
                    );

        } catch (AccessDeniedException ex) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn cần được HLV chấp nhận yêu cầu trước khi nhắn tin."
            );

        } catch (IllegalStateException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage()
            );
        }

        List<ChatMessageResponse> messages =
                chatService.getRecentMessages(
                        member.getId(),
                        conversationId,
                        50
                );

        model.addAttribute(
                "conversationId",
                conversationId
        );

        model.addAttribute(
                "messages",
                messages
        );

        model.addAttribute(
                "partnerName",
                trainer.getUser().getFullName()
        );

        model.addAttribute(
                "partnerAvatarUrl",
                trainer.getUser().getAvatarUrl()
        );

        model.addAttribute(
                "partnerIsTrainer",
                true
        );

        model.addAttribute(
                "conversations",
                chatService.getConversationsForUser(
                        member.getId()
                )
        );

        return "chat/conversation";
    }

    /**
     * URL chat PT cũ, đang dùng trong danh sách PT.
     * Giữ nguyên đường dẫn.
     */
    @GetMapping("/pt/chat/member/{memberId}")
    public String ptChatWithMember(
            @PathVariable("memberId") Long memberId,
            Principal principal,
            Model model
    ) {

        User ptUser = userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Không xác định được HLV"
                        )
                );

        Trainer trainer = trainerRepository
                .findByUser_Id(ptUser.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Tài khoản hiện tại không phải HLV"
                        )
                );

        User member = userRepository
                .findById(memberId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy hội viên"
                        )
                );

        Long conversationId;

        try {
            conversationId =
                    chatService.getOrCreateConversation(
                            member.getId(),
                            trainer.getId()
                    );

        } catch (AccessDeniedException ex) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn chỉ có thể chat với hội viên đã có quan hệ huấn luyện hợp lệ."
            );

        } catch (IllegalStateException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage()
            );
        }

        List<ChatMessageResponse> messages =
                chatService.getRecentMessages(
                        ptUser.getId(),
                        conversationId,
                        50
                );

        model.addAttribute(
                "conversationId",
                conversationId
        );

        model.addAttribute(
                "messages",
                messages
        );

        model.addAttribute(
                "partnerName",
                member.getFullName()
        );

        model.addAttribute(
                "partnerAvatarUrl",
                member.getAvatarUrl()
        );

        model.addAttribute(
                "partnerIsTrainer",
                false
        );

        model.addAttribute(
                "conversations",
                chatService.getConversationsForUser(
                        ptUser.getId()
                )
        );

        return "chat/conversation";
    }
}