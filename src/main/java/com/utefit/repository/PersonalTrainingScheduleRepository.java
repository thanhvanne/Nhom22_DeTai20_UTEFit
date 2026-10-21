package com.utefit.repository;

import com.utefit.entity.PersonalTrainingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PersonalTrainingScheduleRepository extends JpaRepository<PersonalTrainingSchedule, Long> {

    /**
     * Kiểm tra các lịch trùng của HLV trong cùng ngày.
     * Dùng native SQL + CAST cả cột lẫn tham số về time để tránh lỗi
     * "The data types time and datetime are incompatible in the less than or equal to operator."
     */
    @Query(value = """
            SELECT *
            FROM personal_training_schedules pts
            WHERE pts.trainer_id = :trainerId
              AND pts.schedule_date = :date
              AND pts.active = 1
              AND (:excludeId IS NULL OR pts.id <> :excludeId)
              AND NOT (
                    CAST(pts.end_time   AS time) <= CAST(:startTime AS time)
                    OR
                    CAST(pts.start_time AS time) >= CAST(:endTime   AS time)
              )
            """,
            nativeQuery = true)
    List<PersonalTrainingSchedule> findOverlappingForTrainer(
            @Param("trainerId") Long trainerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    /**
     * Kiểm tra các lịch trùng của Member trong cùng ngày.
     */
    @Query(value = """
            SELECT *
            FROM personal_training_schedules pts
            WHERE pts.member_id = :memberId
              AND pts.schedule_date = :date
              AND pts.active = 1
              AND (:excludeId IS NULL OR pts.id <> :excludeId)
              AND NOT (
                    CAST(pts.end_time   AS time) <= CAST(:startTime AS time)
                    OR
                    CAST(pts.start_time AS time) >= CAST(:endTime   AS time)
              )
            """,
            nativeQuery = true)
    List<PersonalTrainingSchedule> findOverlappingForMember(
            @Param("memberId") Long memberId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    /**
     * Lấy lịch active của HLV trong khoảng ngày [fromDate, toDate].
     */
    List<PersonalTrainingSchedule>
    findByTrainer_User_IdAndScheduleDateBetweenAndActiveTrueOrderByScheduleDateAscStartTimeAsc(
            Long trainerUserId,
            LocalDate fromDate,
            LocalDate toDate
    );

    /**
     * Lấy lịch active của Member trong khoảng ngày [fromDate, toDate].
     */
    List<PersonalTrainingSchedule>
    findByMember_IdAndScheduleDateBetweenAndActiveTrueOrderByScheduleDateAscStartTimeAsc(
            Long memberId,
            LocalDate fromDate,
            LocalDate toDate
    );
}