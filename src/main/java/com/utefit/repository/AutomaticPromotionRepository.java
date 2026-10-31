package com.utefit.repository;

import com.utefit.entity.AutomaticPromotion;
import com.utefit.enums.AutomaticPromotionKind;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AutomaticPromotionRepository extends JpaRepository<AutomaticPromotion, AutomaticPromotionKind> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select program from AutomaticPromotion program where program.kind = :kind")
    Optional<AutomaticPromotion> lockByKind(@Param("kind") AutomaticPromotionKind kind);
}
