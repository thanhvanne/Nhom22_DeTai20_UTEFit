package com.utefit.repository;

import com.utefit.entity.LoyaltyPaymentExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Set;

public interface LoyaltyPaymentExclusionRepository extends JpaRepository<LoyaltyPaymentExclusion, Long> {
    @Query("select e.paymentId from LoyaltyPaymentExclusion e where e.paymentId in (select p.id from Payment p where p.user.id = :userId)")
    Set<Long> excludedForUser(@Param("userId") Long userId);
}
