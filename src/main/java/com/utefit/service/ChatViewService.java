package com.utefit.service;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.dto.chat.ConversationSummaryResponse;
import com.utefit.entity.Trainer;
import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatViewService {
    private final ChatService chats;
    private final UserRepository users;
    private final TrainerRepository trainers;
    public record ChatView(Long conversationId, List<ChatMessageResponse> messages,
                           String partnerName, String partnerAvatarUrl,
                           boolean partnerIsTrainer,List<ConversationSummaryResponse> conversations) {}

    @Transactional
    public ChatView memberWithTrainer(String email,Long trainerId) {
        User member=users.findByEmail(email).orElseThrow(()->new AccessDeniedException("Member không tồn tại."));
        if (member.getRole()!=UserRole.MEMBER) throw new AccessDeniedException("Không phải Member.");
        Trainer trainer=trainers.findById(trainerId)
                .orElseThrow(()->new EntityNotFoundException("PT không tồn tại."));
        Long id=chats.getOrCreateConversation(member.getId(),trainer.getId());
        return new ChatView(id,chats.getRecentMessages(member.getId(),id,50),
                trainer.getUser().getFullName(),trainer.getUser().getAvatarUrl(),true,
                chats.getConversationsForUser(member.getId()));
    }
    @Transactional
    public ChatView ptWithMember(String email,Long memberId) {
        User pt=users.findByEmail(email).orElseThrow(()->new AccessDeniedException("PT không tồn tại."));
        if (pt.getRole()!=UserRole.PT) throw new AccessDeniedException("Không phải PT.");
        Trainer trainer=trainers.findByUser_Id(pt.getId())
                .orElseThrow(()->new EntityNotFoundException("Chưa có hồ sơ PT."));
        User member=users.findById(memberId)
                .filter(u->u.getRole()==UserRole.MEMBER)
                .orElseThrow(()->new EntityNotFoundException("Member không tồn tại."));
        Long id=chats.getOrCreateConversation(member.getId(),trainer.getId());
        return new ChatView(id,chats.getRecentMessages(pt.getId(),id,50),
                member.getFullName(),member.getAvatarUrl(),false,
                chats.getConversationsForUser(pt.getId()));
    }
}
