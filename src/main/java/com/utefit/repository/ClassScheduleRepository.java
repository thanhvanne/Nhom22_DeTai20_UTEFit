package com.utefit.repository;

import com.utefit.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassScheduleRepository
        extends JpaRepository<ClassSchedule, Long> {
}