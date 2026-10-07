package com.utefit.repository;

import com.utefit.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository
        extends JpaRepository<Branch, Long> {
}