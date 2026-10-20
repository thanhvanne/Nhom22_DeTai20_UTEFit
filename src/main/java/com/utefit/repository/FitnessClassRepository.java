package com.utefit.repository;

import com.utefit.entity.FitnessClass;
import com.utefit.enums.ClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FitnessClassRepository extends JpaRepository<FitnessClass, Long> {

    @EntityGraph(attributePaths = {"branch", "category", "trainer", "trainer.user"})
    @Query(
            value = """
                    SELECT fc FROM FitnessClass fc
                    JOIN fc.branch b
                    JOIN fc.category c
                    LEFT JOIN fc.trainer t
                    LEFT JOIN t.user u
                    WHERE (:keyword IS NULL
                           OR LOWER(fc.name) LIKE :keyword
                           OR LOWER(fc.description) LIKE :keyword)
                      AND (:branchId IS NULL OR b.id = :branchId)
                      AND (:categoryId IS NULL OR c.id = :categoryId)
                      AND (:trainerId IS NULL OR t.id = :trainerId)
                      AND (:status IS NULL OR fc.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(fc) FROM FitnessClass fc
                    JOIN fc.branch b
                    JOIN fc.category c
                    LEFT JOIN fc.trainer t
                    WHERE (:keyword IS NULL
                           OR LOWER(fc.name) LIKE :keyword
                           OR LOWER(fc.description) LIKE :keyword)
                      AND (:branchId IS NULL OR b.id = :branchId)
                      AND (:categoryId IS NULL OR c.id = :categoryId)
                      AND (:trainerId IS NULL OR t.id = :trainerId)
                      AND (:status IS NULL OR fc.status = :status)
                    """
    )
    Page<FitnessClass> searchAdminClasses(
            @Param("keyword") String keyword,
            @Param("branchId") Long branchId,
            @Param("categoryId") Long categoryId,
            @Param("trainerId") Long trainerId,
            @Param("status") ClassStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"branch", "category", "trainer", "trainer.user"})
    @Query("SELECT fc FROM FitnessClass fc WHERE fc.id = :id")
    Optional<FitnessClass> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"branch", "category", "trainer", "trainer.user"})
    @Query("""
            SELECT fc FROM FitnessClass fc
            WHERE fc.status IN (:statuses)
            ORDER BY fc.name ASC
            """)
    List<FitnessClass> findAllByStatusInOrderByNameAsc(@Param("statuses") List<ClassStatus> statuses);
}