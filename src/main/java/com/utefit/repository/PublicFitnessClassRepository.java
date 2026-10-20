
package com.utefit.repository;

import com.utefit.entity.FitnessClass;
import com.utefit.enums.ClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface PublicFitnessClassRepository
        extends Repository<FitnessClass, Long> {

    @EntityGraph(attributePaths = {
            "branch", "category", "trainer", "trainer.user"
    })
    @Query(
            value = """
            SELECT c FROM FitnessClass c
            WHERE c.branch.active = true
              AND c.category.active = true
              AND c.status IN :statuses
              AND (:branchId IS NULL
                   OR c.branch.id = :branchId)
              AND (:keyword IS NULL
                   OR LOWER(c.name) LIKE :keyword
                   OR LOWER(c.description) LIKE :keyword
                   OR LOWER(c.category.name) LIKE :keyword)
            """,
            countQuery = """
            SELECT COUNT(c) FROM FitnessClass c
            WHERE c.branch.active = true
              AND c.category.active = true
              AND c.status IN :statuses
              AND (:branchId IS NULL
                   OR c.branch.id = :branchId)
              AND (:keyword IS NULL
                   OR LOWER(c.name) LIKE :keyword
                   OR LOWER(c.description) LIKE :keyword
                   OR LOWER(c.category.name) LIKE :keyword)
            """
    )
    Page<FitnessClass> searchPublicClasses(
            @Param("keyword") String keyword,
            @Param("branchId") Long branchId,
            @Param("statuses") Collection<ClassStatus> statuses,
            Pageable pageable
    );
}
