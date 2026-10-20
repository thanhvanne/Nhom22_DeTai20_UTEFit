package com.utefit.repository;

import com.utefit.entity.PersonalTrainingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PersonalTrainingScheduleRepository
        extends JpaRepository<PersonalTrainingSchedule, Long> {

    // Kiểm tra PT bị trùng lịch PT cá nhân (cùng ngày, giờ giao nhau, active = true)
    @Query("""
            SELECT s FROM PersonalTrainingSchedule s
            WHERE s.trainer.id = :trainerId
              AND s.scheduleDate = :scheduleDate
              AND s.active = true
              AND (:excludeId IS NULL OR s.id <> :excludeId)
              AND NOT (s.endTime <= :startTime OR s.startTime >= :endTime)
            """)
    List<PersonalTrainingSchedule> findOverlappingForTrainer(
            @Param("trainerId") Long trainerId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    // Kiểm tra Member bị trùng lịch PT cá nhân (cùng ngày, giờ giao nhau, active = true)
    @Query("""
            SELECT s FROM PersonalTrainingSchedule s
            WHERE s.member.id = :memberId
              AND s.scheduleDate = :scheduleDate
              AND s.active = true
              AND (:excludeId IS NULL OR s.id <> :excludeId)
              AND NOT (s.endTime <= :startTime OR s.startTime >= :endTime)
            """)
    List<PersonalTrainingSchedule> findOverlappingForMember(
            @Param("memberId") Long memberId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    // Lấy lịch PT cá nhân của 1 PT trong khoảng ngày (dùng cho PT dashboard / member view)
    List<PersonalTrainingSchedule> findByTrainer_User_IdAndScheduleDateBetweenAndActiveTrueOrderByScheduleDateAscStartTimeAsc(
            Long trainerUserId,
            LocalDate fromDate,
            LocalDate toDate
    );

    // Lấy lịch PT cá nhân của 1 Member trong khoảng ngày
    List<PersonalTrainingSchedule> findByMember_IdAndScheduleDateBetweenAndActiveTrueOrderByScheduleDateAscStartTimeAsc(
            Long memberUserId,
            LocalDate fromDate,
            LocalDate toDate
    );
}