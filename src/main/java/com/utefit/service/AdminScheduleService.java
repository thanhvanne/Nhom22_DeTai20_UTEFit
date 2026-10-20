package com.utefit.service;

import com.utefit.dto.admin.ClassScheduleFormDto;
import com.utefit.entity.Branch;
import com.utefit.entity.ClassSchedule;
import com.utefit.entity.FitnessClass;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface AdminScheduleService {

    Page<ClassSchedule> getSchedules(
            Long fitnessClassId,
            Long branchId,
            LocalDate scheduleDate,
            Boolean active,
            int page,
            int size
    );

    ClassSchedule getScheduleById(Long id);

    ClassScheduleFormDto getScheduleFormById(Long id);

    void createSchedule(ClassScheduleFormDto dto);

    void updateSchedule(Long id, ClassScheduleFormDto dto);

    void toggleActive(Long id);

    List<FitnessClass> getSchedulableClasses();

    List<Branch> getAllBranches();
}