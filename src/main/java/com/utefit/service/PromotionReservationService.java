package com.utefit.service;

import com.utefit.entity.Membership;
import com.utefit.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionReservationService {
    private final PromotionRepository promotions;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    /** Caller holds membership write lock and performs its PENDING -> CANCELLED transition once. */
    @Transactional
    public void release(Membership membership) {
        if (membership.getPromotionCode() == null) return;
        promotions.lockByCode(membership.getPromotionCode()).ifPresent(p -> {
            entityManager.refresh(p);
            if (p.getQuantity() == Integer.MAX_VALUE) throw new IllegalStateException("Số lượng ưu đãi vượt giới hạn.");
            p.setQuantity(p.getQuantity() + 1);
        });
    }
}
