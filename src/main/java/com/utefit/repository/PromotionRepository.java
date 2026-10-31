
package com.utefit.repository;

import com.utefit.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository
        extends JpaRepository<Promotion, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Promotion p where p.id = :id")
    java.util.Optional<Promotion> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Promotion p where upper(p.code) = upper(:code)")
    java.util.Optional<Promotion> lockByCode(@org.springframework.data.repository.query.Param("code") String code);

    List<Promotion> findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndQuantityGreaterThanOrderByEndDateAsc(
            java.time.LocalDate start, java.time.LocalDate end, Integer quantity);

    boolean existsByCodeIgnoreCase(String code);
    java.util.Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(
            String code, Long id);

    List<Promotion> findAllByOrderByIdDesc();
}
