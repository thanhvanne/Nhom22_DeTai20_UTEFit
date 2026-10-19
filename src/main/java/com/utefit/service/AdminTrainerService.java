package com.utefit.service;

import com.utefit.entity.Branch;
import com.utefit.entity.Trainer;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminTrainerService {

    Page<Trainer> getTrainers(String keyword, Long branchId, Boolean active, int page, int size);

    Trainer getTrainerById(Long id);

    List<Branch> getAllBranches();

    void assignBranch(Long trainerId, Long branchId);

    void toggleActive(Long trainerId);
}