package com.utefit.repository;

import com.utefit.entity.ClassCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassCategoryRepository
        extends JpaRepository<ClassCategory, Long> {
}