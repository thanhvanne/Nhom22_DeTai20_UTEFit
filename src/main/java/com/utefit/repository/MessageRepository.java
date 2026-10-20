package com.utefit.repository;

import com.utefit.entity.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender"})
    List<Message> findTop50ByConversation_IdOrderBySentAtDesc(Long conversationId);

    @EntityGraph(attributePaths = {"sender"})
    Message findTop1ByConversation_IdOrderBySentAtDesc(Long conversationId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    long countUnreadInConversation(@Param("conversationId") Long conversationId,
                                   @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE (m.conversation.member.id = :userId
                   OR m.conversation.trainer.user.id = :userId)
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    long countUnreadForUser(@Param("userId") Long userId);

    @Query("""
            SELECT m FROM Message m
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    List<Message> findUnreadMessagesForUserInConversation(@Param("conversationId") Long conversationId,
                                                          @Param("userId") Long userId);
}