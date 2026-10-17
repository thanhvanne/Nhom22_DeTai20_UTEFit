package com.utefit.repository;

import com.utefit.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    /**
     * Dùng cho dropdown lọc PT ở trang /trainers:
     * Chỉ lấy các chi nhánh đang hoạt động, sắp xếp theo tên.
     */
    List<Branch> findByActiveTrueOrderByNameAsc();
}