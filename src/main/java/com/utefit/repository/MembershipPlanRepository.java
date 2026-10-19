package com.utefit.repository;

import com.utefit.entity.MembershipPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            SELECT p FROM MembershipPlan p
            WHERE (:keyword IS NULL
                   OR LOWER(p.name) LIKE :keyword
                   OR LOWER(p.description) LIKE :keyword)
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<MembershipPlan> searchPlans(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable
    );
}