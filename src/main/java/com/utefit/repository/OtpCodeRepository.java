package com.utefit.repository;

import com.utefit.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository
        extends JpaRepository<OtpCode, Long> {
}