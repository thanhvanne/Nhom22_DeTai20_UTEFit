package com.utefit.repository;

import com.utefit.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    /**
     * Dùng cho dropdown lọc PT ở trang /trainers (của B)
     * và các dropdown chọn chi nhánh đang hoạt động.
     */
    List<Branch> findByActiveTrueOrderByNameAsc();

    /**
     * Dùng cho trang quản lý chi nhánh /admin/branches (của C)
     */
    @Query("""
            SELECT b FROM Branch b
            WHERE (:keyword IS NULL
                   OR LOWER(b.name) LIKE :keyword
                   OR LOWER(b.address) LIKE :keyword
                   OR LOWER(b.phone) LIKE :keyword)
              AND (:active IS NULL OR b.active = :active)
            """)
    Page<Branch> searchBranches(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable
    );

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}