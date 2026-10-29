package com.utefit.service.impl;

import com.utefit.dto.chat.ChatMessageResponse;
import com.utefit.dto.chat.ConversationSummaryResponse;
import com.utefit.entity.*;
import com.utefit.enums.NotificationType;
import com.utefit.enums.TrainerRequestStatus;
import com.utefit.repository.*;
import com.utefit.service.ChatService;
import com.utefit.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;
    private final TrainerRequestRepository trainerRequestRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<ConversationSummaryResponse> getConversationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy người dùng."));

        List<Conversation> asMember =
                conversationRepository.findByMember_IdOrderByUpdatedAtDesc(userId);

        List<Conversation> asTrainer =
                conversationRepository.findByTrainer_User_IdOrderByUpdatedAtDesc(userId);

        List<Conversation> all = new java.util.ArrayList<>();
        all.addAll(asMember);
        all.addAll(asTrainer);

        all.sort(Comparator.comparing(Conversation::getUpdatedAt).reversed());

        return all.stream()
                .map(conv -> buildSummary(conv, user))
                .toList();
    }

    @Override
    public List<ChatMessageResponse> getRecentMessages(Long userId,
                                                       Long conversationId,
                                                       int limit) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy cuộc trò chuyện."));

        if (!isParticipant(conversation, userId)) {
            throw new IllegalStateException("Bạn không có quyền xem cuộc trò chuyện này.");
        }

        if (limit <= 0 || limit > 50) {
            limit = 50;
        }

        List<Message> messages =
                messageRepository.findTop50ByConversation_IdOrderBySentAtDesc(conversationId);

        if (messages.size() > limit) {
            messages = messages.subList(0, limit);
        }

        messages = messages.stream()
                .sorted(Comparator.comparing(Message::getSentAt))
                .toList();

        List<Message> unreadForUser =
                messageRepository.findUnreadMessagesForUserInConversation(conversationId, userId);
        LocalDateTime now = LocalDateTime.now();
        var returnedIds = messages.stream().map(Message::getId).collect(java.util.stream.Collectors.toSet());
        for (Message m : unreadForUser) {
            if (!returnedIds.contains(m.getId())) continue;
            m.setReadAt(now);
        }

        return messages.stream()
                .map(m -> toMessageResponse(m, userId))
                .toList();
    }

    @Override
    public ChatMessageResponse sendMessage(Long senderUserId,
                                           Long conversationId,
                                           String content) {

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        }
        if (content.trim().length() > 2000) {
            throw new IllegalArgumentException("Nội dung tin nhắn tối đa 2000 ký tự.");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy cuộc trò chuyện."));

        if (!isParticipant(conversation, senderUserId)) {
            throw new IllegalStateException("Bạn không có quyền gửi tin nhắn trong cuộc trò chuyện này.");
        }

        Trainer assigned = conversation.getTrainer();
        if (!Boolean.TRUE.equals(assigned.getActive())
                || !Boolean.TRUE.equals(assigned.getUser().getEnabled())
                || assigned.getUser().getStatus() != com.utefit.enums.UserStatus.ACTIVE
                || !trainerRequestRepository.existsByMember_IdAndTrainer_IdAndStatus(
                conversation.getMember().getId(), assigned.getId(), TrainerRequestStatus.ACCEPTED)) {
            throw new IllegalStateException("Quan hệ Member–PT không còn hiệu lực để nhắn tin.");
        }

        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy người gửi."));
        if (sender.getStatus() != com.utefit.enums.UserStatus.ACTIVE || !Boolean.TRUE.equals(sender.getEnabled()))
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản không hoạt động.");
        User receiver = resolveReceiver(conversation, senderUserId);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content.trim())
                .sentAt(LocalDateTime.now())
                .build();

        Message saved = messageRepository.save(message);

        conversation.setUpdatedAt(LocalDateTime.now());

        // Thông báo (Notification)
        if (receiver != null) {
            String title = "Tin nhắn mới";
            String preview = content.trim();
            if (preview.length() > 80) {
                preview = preview.substring(0, 77) + "...";
            }

            notificationService.createNotificationForUser(
                    receiver.getId(),
                    NotificationType.MESSAGE_RECEIVED,
                    title,
                    preview,
                    conversation.getId()
            );
        }

        ChatMessageResponse response = toMessageResponse(saved, senderUserId);
        // Người nhận phải có mine=false, không dùng response của người gửi.
        ChatMessageResponse receiverResponse = receiver == null
                ? null : toMessageResponse(saved, receiver.getId());

        // Không phát sự kiện WebSocket trước khi transaction lưu Message commit.
        String senderEmail = sender.getEmail();
        String receiverEmail = receiver == null ? null : receiver.getEmail();
        Runnable push = () -> {
            try {
                messagingTemplate.convertAndSendToUser(senderEmail, "/queue/chat", response);
                if (receiverEmail != null) {
                    messagingTemplate.convertAndSendToUser(receiverEmail, "/queue/chat", receiverResponse);
                }
            } catch (RuntimeException ignored) {
                // Tin nhắn vẫn ở DB, client tải lại lịch sử.
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { push.run(); }
            });
        } else push.run();

        return response;
    }

    @Override
    public Long getOrCreateConversation(Long memberUserId, Long trainerId) {
        User member = userRepository.findById(memberUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy thành viên."));

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy huấn luyện viên."));

        if (!Boolean.TRUE.equals(trainer.getActive())
                || !Boolean.TRUE.equals(trainer.getUser().getEnabled())
                || trainer.getUser().getStatus() != com.utefit.enums.UserStatus.ACTIVE) {
            throw new IllegalStateException("PT đã ngừng hoạt động; không thể bắt đầu Chat mới.");
        }
        boolean hasAccepted = trainerRequestRepository
                .existsByMember_IdAndTrainer_IdAndStatus(
                        member.getId(),
                        trainer.getId(),
                        TrainerRequestStatus.ACCEPTED
                );

        if (!hasAccepted) {
            throw new IllegalStateException(
                    "Chỉ được bắt đầu trò chuyện sau khi yêu cầu huấn luyện viên đã được chấp nhận."
            );
        }

        return conversationRepository
                .findByMember_IdAndTrainer_Id(member.getId(), trainer.getId())
                .map(Conversation::getId)
                .orElseGet(() -> {
                    Conversation conv = Conversation.builder()
                            .member(member)
                            .trainer(trainer)
                            .createdAt(LocalDateTime.now())
                            .build();
                    Conversation saved = conversationRepository.save(conv);
                    return saved.getId();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessagesForUser(Long userId) {
        return messageRepository.countUnreadForUser(userId);
    }

    // ===== Helpers =====

    private boolean isParticipant(Conversation conv, Long userId) {
        Long memberId = conv.getMember().getId();
        Long trainerUserId = conv.getTrainer().getUser().getId();
        return memberId.equals(userId) || trainerUserId.equals(userId);
    }

    private User resolveReceiver(Conversation conv, Long senderUserId) {
        User member = conv.getMember();
        User trainerUser = conv.getTrainer().getUser();

        if (member.getId().equals(senderUserId)) {
            return trainerUser;
        }
        if (trainerUser.getId().equals(senderUserId)) {
            return member;
        }
        return null;
    }

    private ChatMessageResponse toMessageResponse(Message m, Long currentUserId) {
        User sender = m.getSender();
        boolean mine = sender.getId().equals(currentUserId);

        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .readAt(m.getReadAt())
                .mine(mine)
                .build();
    }

    private ConversationSummaryResponse buildSummary(Conversation conv, User currentUser) {
        User member = conv.getMember();
        Trainer trainer = conv.getTrainer();
        User trainerUser = trainer.getUser();

        boolean currentIsMember = member.getId().equals(currentUser.getId());

        User partnerUser;
        boolean partnerIsTrainer;

        if (currentIsMember) {
            partnerUser = trainerUser;
            partnerIsTrainer = true;
        } else {
            partnerUser = member;
            partnerIsTrainer = false;
        }

        long unreadCount =
                messageRepository.countUnreadInConversation(conv.getId(), currentUser.getId());

        Message lastMessage =
                messageRepository.findTop1ByConversation_IdOrderBySentAtDesc(conv.getId());

        String preview = null;
        LocalDateTime lastAt = null;
        if (lastMessage != null) {
            preview = lastMessage.getContent();
            if (preview != null && preview.length() > 80) {
                preview = preview.substring(0, 77) + "...";
            }
            lastAt = lastMessage.getSentAt();
        }

        return ConversationSummaryResponse.builder()
                .conversationId(conv.getId())
                .partnerUserId(partnerUser.getId())
                .partnerName(partnerUser.getFullName())
                .partnerAvatarUrl(partnerUser.getAvatarUrl())
                .partnerIsTrainer(partnerIsTrainer)
                .lastMessagePreview(preview)
                .lastMessageAt(lastAt)
                .unreadCount(unreadCount)
                .build();
    }
}