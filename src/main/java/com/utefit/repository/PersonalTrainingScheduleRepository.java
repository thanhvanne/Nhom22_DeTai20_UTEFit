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
    List<PersonalTrainingSchedule> findByTrainer_User_IdAndScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(Long userId, LocalDate from, LocalDate to);
    @Query("select distinct p.member.id from PersonalTrainingSchedule p where p.active = true and (p.status is null or p.status = com.utefit.enums.PersonalTrainingStatus.SCHEDULED) and p.scheduleDate >= :today order by p.member.id")
    List<Long> findFutureMemberIds(@Param("today") LocalDate today, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"trainer", "trainer.branch", "trainer.user", "branch"})
    @Query("select p from PersonalTrainingSchedule p where p.member.id = :memberId and p.active = true and (p.status is null or p.status = com.utefit.enums.PersonalTrainingStatus.SCHEDULED) and p.scheduleDate >= :today order by p.id")
    List<PersonalTrainingSchedule> findFutureForMember(@Param("memberId") Long memberId, @Param("today") LocalDate today);

    @Query("""
            SELECT p FROM PersonalTrainingSchedule p
            JOIN FETCH p.member JOIN FETCH p.trainer t JOIN FETCH t.user
            WHERE p.active = true AND p.status = :status
              AND p.scheduleDate BETWEEN :first AND :last
            """)
    List<PersonalTrainingSchedule> findUpcomingForReminders(
            @Param("status") com.utefit.enums.PersonalTrainingStatus status,
            @Param("first") LocalDate first, @Param("last") LocalDate last);

    @Query(value = """
            SELECT *
            FROM personal_training_schedules pts
            WHERE pts.trainer_id = :trainerId
              AND pts.schedule_date = :date
              AND pts.active = 1
              AND (:excludeId IS NULL OR pts.id <> :excludeId)
              AND NOT (
                  CAST(pts.end_time AS time) <= CAST(:startTime AS time)
                  OR CAST(pts.start_time AS time) >= CAST(:endTime AS time)
              )
            """, nativeQuery = true)
    List<PersonalTrainingSchedule> findOverlappingForTrainer(
            @Param("trainerId") Long trainerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    List<PersonalTrainingSchedule>
    findByMember_IdAndScheduleDateAndActiveTrue(
            Long memberId,
            LocalDate scheduleDate
    );

    boolean existsByTrainer_IdAndActiveTrueAndScheduleDateAfter(
            Long trainerId,
            LocalDate date
    );

    List<PersonalTrainingSchedule>
    findByTrainer_IdAndScheduleDateAndActiveTrue(
            Long trainerId,
            LocalDate scheduleDate
    );

    @Query(value = """
            SELECT *
            FROM personal_training_schedules pts
            WHERE pts.member_id = :memberId
              AND pts.schedule_date = :date
              AND pts.active = 1
              AND (:excludeId IS NULL OR pts.id <> :excludeId)
              AND NOT (
                  CAST(pts.end_time AS time) <= CAST(:startTime AS time)
                  OR CAST(pts.start_time AS time) >= CAST(:endTime AS time)
              )
            """, nativeQuery = true)
    List<PersonalTrainingSchedule> findOverlappingForMember(
            @Param("memberId") Long memberId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    List<PersonalTrainingSchedule>
    findByTrainer_User_IdAndScheduleDateBetweenAndActiveTrueOrderByScheduleDateAscStartTimeAsc(
            Long trainerUserId,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<PersonalTrainingSchedule>
    findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(
            Long memberId,
            LocalDate fromDate,
            LocalDate toDate
    );
}
