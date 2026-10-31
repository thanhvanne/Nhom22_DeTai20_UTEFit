package com.utefit.service;

import com.utefit.config.LoyaltyProperties;
import com.utefit.entity.AutomaticPromotion;
import com.utefit.enums.AutomaticPromotionKind;
import com.utefit.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutomaticPromotionServiceTest {
    @Test void stateChangeValidatesOnlyItsLockedRowToAvoidCrossProgramLockInversion() {
        var repository = mock(AutomaticPromotionRepository.class);
        var service = new AutomaticPromotionService(repository, mock(MembershipRepository.class), new LoyaltyProperties(), Clock.systemUTC());
        ReflectionTestUtils.setField(service, "entityManager", mock(EntityManager.class));
        var row = AutomaticPromotion.builder().kind(AutomaticPromotionKind.WELCOME).version(1L).active(false)
                .percent(new BigDecimal("10")).discountCap(new BigDecimal("50000")).build();
        when(repository.lockByKind(AutomaticPromotionKind.WELCOME)).thenReturn(Optional.of(row));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin@unit.test", "unused", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            service.setActive(AutomaticPromotionKind.WELCOME, 1, true);
            assertTrue(row.isActive()); verify(repository, never()).findAll(); verify(repository).saveAndFlush(row);
            assertThrows(IllegalStateException.class, () -> service.setActive(AutomaticPromotionKind.WELCOME, 0, false));
            assertTrue(row.isActive());
        } finally { SecurityContextHolder.clearContext(); }
    }
}
