
package com.utefit.repository;

import com.utefit.entity.ClassSchedule;
import com.utefit.enums.ClassStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface MemberClassScheduleRepository
        extends JpaRepository<ClassSchedule, Long> {

    // Lấy lịch tương lai, chỉ từ chi nhánh và
    // danh mục đang hoạt động.
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
            JOIN fc.category cat
            WHERE cat.active = true
              AND s.active = true
              AND b.active = true
              AND s.scheduleDate >= :fromDate
              AND fc.status IN :statuses
              AND (:branchId IS NULL OR b.id = :branchId)
              AND (:keyword IS NULL
                   OR LOWER(fc.name) LIKE :keyword)
            ORDER BY
                s.scheduleDate ASC,
                s.startTime ASC,
                s.id ASC
            """,
            countQuery = """
            SELECT COUNT(s) FROM ClassSchedule s
            JOIN s.fitnessClass fc
            JOIN fc.branch b
            JOIN fc.category cat
            WHERE cat.active = true
              AND s.active = true
              AND b.active = true
              AND s.scheduleDate >= :fromDate
              AND fc.status IN :statuses
              AND (:branchId IS NULL OR b.id = :branchId)
              AND (:keyword IS NULL
                   OR LOWER(fc.name) LIKE :keyword)
            """
    )
    Page<ClassSchedule> searchMemberSchedules(
            @Param("fromDate") LocalDate fromDate,
            @Param("statuses") Collection<ClassStatus> statuses,
            @Param("branchId") Long branchId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Khóa lịch trong lúc kiểm tra số suất còn lại.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.id = :id
            """)
    Optional<ClassSchedule> lockById(
            @Param("id") Long id
    );
}
