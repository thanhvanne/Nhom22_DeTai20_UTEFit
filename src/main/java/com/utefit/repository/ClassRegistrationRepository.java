package com.utefit.repository;

import com.utefit.entity.ClassRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRegistrationRepository
        extends JpaRepository<ClassRegistration, Long> {
}