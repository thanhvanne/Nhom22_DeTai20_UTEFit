package com.utefit.repository;

import com.utefit.entity.ClassSchedule;
import com.utefit.enums.ClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    @EntityGraph(attributePaths = {
            "fitnessClass",
            "fitnessClass.branch",
            "fitnessClass.category",
            "fitnessClass.trainer",
            "fitnessClass.trainer.user"
    })
    @Query(
            value = """
                    SELECT s FROM ClassSchedule s
                    JOIN s.fitnessClass fc
                    JOIN fc.branch b
                    LEFT JOIN fc.trainer t
                    WHERE (:fitnessClassId IS NULL OR fc.id = :fitnessClassId)
                      AND (:branchId IS NULL OR b.id = :branchId)
                      AND (:scheduleDate IS NULL OR s.scheduleDate = :scheduleDate)
                      AND (:active IS NULL OR s.active = :active)
                    """,
            countQuery = """
                    SELECT COUNT(s) FROM ClassSchedule s
                    JOIN s.fitnessClass fc
                    JOIN fc.branch b
                    WHERE (:fitnessClassId IS NULL OR fc.id = :fitnessClassId)
                      AND (:branchId IS NULL OR b.id = :branchId)
                      AND (:scheduleDate IS NULL OR s.scheduleDate = :scheduleDate)
                      AND (:active IS NULL OR s.active = :active)
                    """
    )
    Page<ClassSchedule> searchAdminSchedules(
            @Param("fitnessClassId") Long fitnessClassId,
            @Param("branchId") Long branchId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "fitnessClass",
            "fitnessClass.branch",
            "fitnessClass.category",
            "fitnessClass.trainer",
            "fitnessClass.trainer.user"
    })
    @Query("SELECT s FROM ClassSchedule s WHERE s.id = :id")
    Optional<ClassSchedule> findDetailById(@Param("id") Long id);

    // Ép kiểu CAST(... AS time) để tương thích tuyệt đối với kiểu dữ liệu time của SQL Server
    @EntityGraph(attributePaths = {"fitnessClass", "fitnessClass.branch"})
    @Query("""
            SELECT s FROM ClassSchedule s
            JOIN s.fitnessClass fc
            JOIN fc.trainer t
            WHERE t.id = :trainerId
              AND s.scheduleDate = :scheduleDate
              AND s.active = true
              AND fc.status <> :cancelledStatus
              AND s.id <> :excludeScheduleId
              AND NOT (s.endTime <= CAST(:startTime AS time) OR s.startTime >= CAST(:endTime AS time))
            """)
    List<ClassSchedule> findOverlappingTrainerSchedules(
            @Param("trainerId") Long trainerId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("cancelledStatus") ClassStatus cancelledStatus,
            @Param("excludeScheduleId") Long excludeScheduleId
    );

    @EntityGraph(attributePaths = {"fitnessClass"})
    @Query("""
            SELECT s FROM ClassSchedule s
            JOIN s.fitnessClass fc
            JOIN fc.branch b
            WHERE b.id = :branchId
              AND s.scheduleDate = :scheduleDate
              AND s.active = true
              AND fc.status <> :cancelledStatus
              AND s.room IS NOT NULL
              AND LOWER(s.room) = LOWER(:room)
              AND s.id <> :excludeScheduleId
              AND NOT (s.endTime <= CAST(:startTime AS time) OR s.startTime >= CAST(:endTime AS time))
            """)
    List<ClassSchedule> findOverlappingRoomSchedules(
            @Param("branchId") Long branchId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("room") String room,
            @Param("cancelledStatus") ClassStatus cancelledStatus,
            @Param("excludeScheduleId") Long excludeScheduleId
    );

    List<ClassSchedule> findByFitnessClassIdAndActiveTrue(Long fitnessClassId);

    // === Thêm mới cho PT xem lịch lớp của mình ===
    @EntityGraph(attributePaths = {
            "fitnessClass",
            "fitnessClass.branch",
            "fitnessClass.category",
            "fitnessClass.trainer",
            "fitnessClass.trainer.user"
    })
    @Query("""
            SELECT s FROM ClassSchedule s
            JOIN s.fitnessClass fc
            JOIN fc.trainer t
            JOIN t.user u
            WHERE u.id = :trainerUserId
              AND s.active = true
              AND fc.status <> :cancelledStatus
              AND s.scheduleDate >= :fromDate
            ORDER BY s.scheduleDate ASC, s.startTime ASC
            """)
    Page<ClassSchedule> findUpcomingSchedulesForTrainer(
            @Param("trainerUserId") Long trainerUserId,
            @Param("cancelledStatus") ClassStatus cancelledStatus,
            @Param("fromDate") LocalDate fromDate,
            Pageable pageable
    );
}