package com.utefit.repository;

import com.utefit.entity.Notification;
import com.utefit.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    org.springframework.data.domain.Page<Notification> findByUser_IdOrderByCreatedAtDescIdDesc(Long id, org.springframework.data.domain.Pageable page);

    boolean existsByUser_IdAndTypeAndRelatedIdAndTitle(Long userId, NotificationType type,
                                                       Long relatedId, String title);

    // Đếm số thông báo chưa đọc cho user
    long countByUser_IdAndReadIsFalse(Long userId);

    // Lấy tối đa 20 thông báo mới nhất
    List<Notification> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);

    // Lấy tất cả thông báo của user, mới nhất trước
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // Lấy tất cả thông báo chưa đọc của user
    List<Notification> findByUser_IdAndReadIsFalseOrderByCreatedAtDesc(Long userId);

    // Tìm theo id + userId để check quyền sở hữu
    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);
}
