package com.utefit.repository;

import com.utefit.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByMember_IdAndTrainer_Id(Long memberId, Long trainerId);

    List<Conversation> findByMember_IdOrderByUpdatedAtDesc(Long memberId);

    List<Conversation> findByTrainer_User_IdOrderByUpdatedAtDesc(Long trainerUserId);
}