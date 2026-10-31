package com.utefit.service;

import com.utefit.Nhom22DeTai20UteFitApplication;
import com.utefit.entity.AutomaticPromotion;
import com.utefit.enums.AutomaticPromotionKind;
import com.utefit.repository.AutomaticPromotionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticPromotionRestartTest {
    @Test void savedConfigurationSurvivesFullApplicationRestartWithEnvironmentFlagFalse() {
        String database = "jdbc:h2:mem:program_restart_" + UUID.randomUUID() + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        var arguments = new ArrayList<>(List.of("--spring.config.location=file:src/test/resources/application.properties",
                "--spring.datasource.url=" + database, "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa", "--spring.datasource.password=", "--spring.profiles.active=",
                "--server.address=127.0.0.1", "--server.port=0", "--utefit.loyalty.enabled=false",
                "--SEPAY_ENABLED=false", "--SEPAY_LIVE_ENABLED=false", "--SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false",
                "--utefit.scheduling.enabled=false", "--spring.jpa.hibernate.ddl-auto=create"));
        try {
            try (var first = new SpringApplicationBuilder(Nhom22DeTai20UteFitApplication.class).web(WebApplicationType.SERVLET).run(arguments.toArray(String[]::new))) {
                first.getBean(AutomaticPromotionRepository.class).saveAndFlush(AutomaticPromotion.builder().kind(AutomaticPromotionKind.WELCOME).active(false)
                        .percent(new BigDecimal("10")).discountCap(new BigDecimal("50000")).updatedAt(LocalDateTime.now()).updatedBy("H2 fixture").build());
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin@restart-h2.test", "unused", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
                var service = first.getBean(AutomaticPromotionService.class);
                var form = service.form(AutomaticPromotionKind.WELCOME); form.setPercent(new BigDecimal("15")); form.setActive(true);
                service.save(AutomaticPromotionKind.WELCOME, form);
            }
            arguments.set(arguments.size() - 1, "--spring.jpa.hibernate.ddl-auto=validate");
            try (var second = new SpringApplicationBuilder(Nhom22DeTai20UteFitApplication.class).web(WebApplicationType.SERVLET).run(arguments.toArray(String[]::new))) {
                var rule = second.getBean(AutomaticPromotionService.class).settings().rule(AutomaticPromotionKind.WELCOME);
                assertTrue(rule.persisted()); assertTrue(rule.available()); assertEquals(0, new BigDecimal("15").compareTo(rule.percent()));
                assertEquals("admin@restart-h2.test", rule.updatedBy()); assertTrue(rule.version() > 0);
            }
        } finally { SecurityContextHolder.clearContext(); }
    }
}
