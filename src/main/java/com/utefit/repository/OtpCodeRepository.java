package com.utefit.repository;

import com.utefit.entity.OtpCode;
import com.utefit.entity.User;
import com.utefit.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpCodeRepository
        extends JpaRepository<OtpCode, Long> {

    List<OtpCode> findByUserAndTypeAndUsedFalse(
            User user,
            OtpType type
    );

    Optional<OtpCode> findTopByUserAndTypeOrderByCreatedAtDesc(
            User user,
            OtpType type
    );

    Optional<OtpCode> findTopByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(
            User user,
            OtpType type
    );

    long countByUserAndTypeAndCreatedAtAfter(
            User user,
            OtpType type,
            LocalDateTime after
    );
}