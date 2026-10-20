
package com.utefit.repository;

import com.utefit.entity.ClassRegistration;
import com.utefit.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassRegistrationRepository
        extends JpaRepository<ClassRegistration, Long> {

    // Hàm cũ: PT đếm Member đang đăng ký.
    long countBySchedule_IdAndStatus(
            Long scheduleId,
            RegistrationStatus status
    );

    // Hàm cũ: PT xem danh sách Member.
    @EntityGraph(attributePaths = {"user"})
    List<ClassRegistration> findBySchedule_IdAndStatusOrderByRegisteredAtAsc(
            Long scheduleId,
            RegistrationStatus status
    );

    // Tìm bản ghi cũ, kể cả CANCELLED.
    // Phục vụ đăng ký lại cùng lịch.
    Optional<ClassRegistration> findByUser_IdAndSchedule_Id(
            Long userId,
            Long scheduleId
    );

    // Các lớp Member đã đăng ký trong cùng ngày.
    // Dùng để kiểm tra trùng giờ.
    @EntityGraph(attributePaths = {
            "schedule",
            "schedule.fitnessClass"
    })
    @Query("""
            SELECT r FROM ClassRegistration r
            JOIN r.schedule s
            WHERE r.user.id = :userId
              AND r.status = :status
              AND s.scheduleDate = :scheduleDate
              AND s.id <> :excludeScheduleId
            """)
    List<ClassRegistration> findOtherRegistrationsOnDate(
            @Param("userId") Long userId,
            @Param("status") RegistrationStatus status,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("excludeScheduleId") Long excludeScheduleId
    );

    // Lịch sử đặt lớp của chính Member.
    @EntityGraph(attributePaths = {
            "schedule",
            "schedule.fitnessClass",
            "schedule.fitnessClass.branch",
            "schedule.fitnessClass.category"
    })
    @Query("""
            SELECT r FROM ClassRegistration r
            JOIN r.schedule s
            WHERE r.user.id = :userId
            ORDER BY s.scheduleDate DESC, s.startTime DESC
            """)
    List<ClassRegistration> findHistoryByUserId(
            @Param("userId") Long userId
    );
}
