package com.utefit.repository;

import com.utefit.entity.MembershipPlan;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @Query("select p from MembershipPlan p where p.id = :id")
    java.util.Optional<MembershipPlan> lockForSnapshot(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MembershipPlan p where p.id = :id")
    java.util.Optional<MembershipPlan> lockForUpdate(@Param("id") Long id);

    List<MembershipPlan> findByActiveTrueOrderByNameAsc();

    @Query("""
        select p from MembershipPlan p where p.active = true
        and (p.accessScope is null or p.accessScope = com.utefit.enums.MembershipScope.SINGLE_BRANCH)
        and size(p.branchIds) = 1
        and exists (select b.id from Branch b where b.active = true and b.id member of p.branchIds)
        and (:keyword is null or lower(p.name) like :keyword or lower(p.description) like :keyword)
        """)
    Page<MembershipPlan> searchFixedPlans(@Param("keyword") String keyword, Pageable pageable);

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
