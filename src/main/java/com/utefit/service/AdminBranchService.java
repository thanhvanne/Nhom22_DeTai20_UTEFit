package com.utefit.service;

import com.utefit.dto.admin.BranchFormDto;
import com.utefit.entity.Branch;
import org.springframework.data.domain.Page;

public interface AdminBranchService {
    Page<Branch> getBranches(String keyword, Boolean active, int page, int size);
    Branch getBranchById(Long id);
    BranchFormDto getBranchFormById(Long id);
    void createBranch(BranchFormDto dto);
    void updateBranch(Long id, BranchFormDto dto);
    void toggleActive(Long id);
}