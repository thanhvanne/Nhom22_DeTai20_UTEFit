package com.utefit.repository;

import com.utefit.entity.ClassCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassCategoryRepository extends JpaRepository<ClassCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            SELECT c FROM ClassCategory c
            WHERE (:keyword IS NULL
                   OR LOWER(c.name) LIKE :keyword
                   OR LOWER(c.description) LIKE :keyword)
              AND (:active IS NULL OR c.active = :active)
            """)
    Page<ClassCategory> searchCategories(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable
    );
}