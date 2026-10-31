package com.utefit.validation;

import com.utefit.Nhom22DeTai20UteFitApplication;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.sql.DataSource;
import java.time.LocalTime;

public class ValidationBrowserApplication {
    public static void main(String[] args) {
        new SpringApplication(Nhom22DeTai20UteFitApplication.class, Fixtures.class).run(
                "--spring.config.location=file:src/test/resources/application.properties",
                "--server.port=18081",
                "--spring.datasource.url=jdbc:h2:mem:utefit_validation_browser;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--spring.jpa.hibernate.ddl-auto=create-drop", "--utefit.scheduling.enabled=false");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean @org.springframework.context.annotation.Primary com.utefit.service.MailService validationBrowserMail() {
            return org.mockito.Mockito.mock(com.utefit.service.MailService.class);
        }

        @Bean ApplicationRunner validationBrowserFixtures(DataSource datasource, UserRepository users, BranchRepository branches,
                                                         TrainerRepository trainers, ManagerBranchAssignmentRepository assignments,
                                                         PasswordEncoder encoder) {
            return arguments -> {
                try (var connection = datasource.getConnection()) {
                    if (!connection.getMetaData().getURL().startsWith("jdbc:h2:mem:utefit_validation_browser")) throw new IllegalStateException("Browser fixtures require isolated in-memory H2.");
                }
                Branch branch = branches.saveAndFlush(Branch.builder().name("Chi nhánh kiểm thử").address("Thủ Đức")
                        .phone("02800001001").openingTime(LocalTime.of(6,0)).closingTime(LocalTime.of(22,0)).active(true).build());
                for (UserRole role : UserRole.values()) {
                    User user = users.saveAndFlush(User.builder().email("validation."+role.name().toLowerCase(java.util.Locale.ROOT)+"@utefit.test")
                            .firstName("Nguyễn").lastName("Kiểm Thử").phone("0901234567").password(encoder.encode("Validation-test-123"))
                            .role(role).status(UserStatus.ACTIVE).enabled(true).tokenVersion(0).build());
                    if (role == UserRole.PT) trainers.saveAndFlush(Trainer.builder().user(user).branch(branch).specialization("Thể lực")
                            .yearsExperience(5).bio("Hồ sơ kiểm thử validation.").active(true).build());
                    if (role == UserRole.MANAGER) assignments.saveAndFlush(ManagerBranchAssignment.builder().manager(user).branch(branch).build());
                }
            };
        }
    }
}
