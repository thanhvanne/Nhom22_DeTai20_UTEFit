package com.utefit.repository;

import com.utefit.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUser_IdAndReadIsFalse(Long userId);

    List<Notification> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUser_IdAndReadIsFalseOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);
}