
package com.utefit.repository;

import com.utefit.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository
        extends JpaRepository<Promotion, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(
            String code, Long id);

    List<Promotion> findAllByOrderByIdDesc();
}
