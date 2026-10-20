package com.utefit.service;

import com.utefit.dto.admin.FitnessClassFormDto;
import com.utefit.entity.Branch;
import com.utefit.entity.ClassCategory;
import com.utefit.entity.FitnessClass;
import com.utefit.entity.Trainer;
import com.utefit.enums.ClassStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminClassService {

    Page<FitnessClass> getClasses(
            String keyword,
            Long branchId,
            Long categoryId,
            Long trainerId,
            ClassStatus status,
            int page,
            int size
    );

    FitnessClass getClassById(Long id);

    FitnessClassFormDto getClassFormById(Long id);

    void createClass(FitnessClassFormDto dto);

    void updateClass(Long id, FitnessClassFormDto dto);

    void cancelClass(Long id);

    List<Branch> getActiveBranches();

    List<ClassCategory> getActiveCategories();

    List<Trainer> getActiveTrainers();
}