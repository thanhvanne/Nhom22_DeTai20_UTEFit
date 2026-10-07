package com.utefit.repository;

import com.utefit.entity.FitnessClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FitnessClassRepository
        extends JpaRepository<FitnessClass, Long> {
}