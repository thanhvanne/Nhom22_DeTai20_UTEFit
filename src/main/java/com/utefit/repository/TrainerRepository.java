package com.utefit.repository;

import com.utefit.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainerRepository
        extends JpaRepository<Trainer, Long> {
}