package com.utefit.repository;

import com.utefit.entity.ClassRegistration;
import com.utefit.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassRegistrationRepository extends JpaRepository<ClassRegistration, Long> {

    long countBySchedule_IdAndStatus(Long scheduleId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<ClassRegistration> findBySchedule_IdAndStatusOrderByRegisteredAtAsc(Long scheduleId,
                                                                             RegistrationStatus status);
}