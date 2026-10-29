package com.utefit.repository;

import com.utefit.entity.ClassRegistration;
import com.utefit.enums.RegistrationStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassRegistrationRepository
        extends JpaRepository<ClassRegistration, Long> {
    long countBySchedule_IdAndStatusNot(Long id, RegistrationStatus status);
    @EntityGraph(attributePaths = {"user"})
    List<ClassRegistration> findBySchedule_IdAndStatusNotOrderByRegisteredAtAsc(Long scheduleId, RegistrationStatus excluded);
    @Query("select distinct r.user.id from ClassRegistration r where r.status = com.utefit.enums.RegistrationStatus.REGISTERED and r.schedule.scheduleDate >= :today order by r.user.id")
    List<Long> findFutureMemberIds(@Param("today") LocalDate today, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"schedule", "schedule.fitnessClass", "schedule.fitnessClass.branch", "schedule.fitnessClass.category"})
    @Query("select r from ClassRegistration r where r.user.id = :memberId and r.status = com.utefit.enums.RegistrationStatus.REGISTERED and r.schedule.scheduleDate >= :today order by r.schedule.id")
    List<ClassRegistration> findFutureForMember(@Param("memberId") Long memberId, @Param("today") LocalDate today);

    @Query("""
            SELECT r FROM ClassRegistration r
            JOIN FETCH r.user u JOIN FETCH r.schedule s JOIN FETCH s.fitnessClass fc
            WHERE r.status = :registered AND s.scheduleDate BETWEEN :first AND :last
              AND s.active = true AND fc.status <> :cancelled
            """)
    List<ClassRegistration> findUpcomingForReminders(
            @Param("registered") RegistrationStatus registered,
            @Param("cancelled") com.utefit.enums.ClassStatus cancelled,
            @Param("first") LocalDate first, @Param("last") LocalDate last);

    // =====================================================
    // GIAI DOAN 5 - ADMIN NOTIFICATION
    // =====================================================

    // Lay danh sach ID Member da dang ky mot lich hoc.
    // Chi lay Member co trang thai REGISTERED.
    // Khong lay Member da huy dang ky.
    @Query("""
            SELECT DISTINCT r.user.id
            FROM ClassRegistration r
            WHERE r.schedule.id = :scheduleId
              AND r.status = :status
            """)
    List<Long> findDistinctUserIdsByScheduleAndStatus(
            @Param("scheduleId") Long scheduleId,
            @Param("status") RegistrationStatus status
    );

    // Lay danh sach ID Member da dang ky lop hoc.
    // Mot FitnessClass co the co nhieu ClassSchedule.
    // DISTINCT giup moi Member chi nhan mot thong bao
    // khi Admin huy toan bo lop hoc.
    @Query("""
            SELECT DISTINCT r.user.id
            FROM ClassRegistration r
            WHERE r.schedule.fitnessClass.id = :fitnessClassId
              AND r.status = :status
            """)
    List<Long> findDistinctUserIdsByClassAndStatus(
            @Param("fitnessClassId") Long fitnessClassId,
            @Param("status") RegistrationStatus status
    );

    // =====================================================
    // CAC PHUONG THUC CU - GIU NGUYEN
    // =====================================================

    @Modifying
    @Query("UPDATE ClassRegistration r SET r.status = :target WHERE r.schedule.id = :scheduleId AND r.status = :current")
    int cancelRegistrationsForSchedule(@Param("scheduleId") Long scheduleId,
                                       @Param("current") RegistrationStatus current,
                                       @Param("target") RegistrationStatus target);

    @Modifying
    @Query("UPDATE ClassRegistration r SET r.status = :target WHERE r.id IN (SELECT x.id FROM ClassRegistration x WHERE x.schedule.fitnessClass.id = :classId AND x.status = :current AND (x.schedule.scheduleDate > :day OR (x.schedule.scheduleDate = :day AND x.schedule.startTime > :time)))")
    int cancelRegistrationsForClass(@Param("classId") Long classId, @Param("day") java.time.LocalDate day, @Param("time") java.time.LocalTime time,
                                    @Param("current") RegistrationStatus current,
                                    @Param("target") RegistrationStatus target);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ClassRegistration r JOIN FETCH r.schedule s JOIN FETCH s.fitnessClass fc WHERE r.id = :id")
    Optional<ClassRegistration> lockForAttendance(@Param("id") Long id);

    // PT dem so Member dang dang ky.
    long countBySchedule_IdAndStatus(
            Long scheduleId,
            RegistrationStatus status
    );

    // PT xem danh sach Member.
    @EntityGraph(attributePaths = {"user"})
    List<ClassRegistration> findBySchedule_IdAndStatusOrderByRegisteredAtAsc(
            Long scheduleId,
            RegistrationStatus status
    );

    // Tim ban ghi cu, ke ca CANCELLED.
    // Phuc vu dang ky lai cung lich.
    Optional<ClassRegistration> findByUser_IdAndSchedule_Id(
            Long userId,
            Long scheduleId
    );

    // Cac lop Member da dang ky trong cung ngay.
    // Dung de kiem tra trung gio.
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

    // Lich su dat lop cua chinh Member.
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
