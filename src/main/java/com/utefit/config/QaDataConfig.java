package com.utefit.config;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Configuration
@Profile("qa")
public class QaDataConfig {
    @Bean
    static BeanFactoryPostProcessor qaDatabaseGuard(Environment environment) {
        return factory -> {
            String url = environment.getRequiredProperty("spring.datasource.url");
            if (!url.contains("databaseName=UTEFitQA_GD7_20261025;")) {
                throw new IllegalStateException("QA profile chỉ được dùng database UTEFitQA_GD7_20261025.");
            }
        };
    }

    @Bean
    CommandLineRunner qaFixtures(UserRepository users, BranchRepository branches,
                                 MembershipPlanRepository plans, ClassCategoryRepository categories,
                                 TrainerRepository trainers, FitnessClassRepository classes,
                                 ClassScheduleRepository schedules, PasswordEncoder encoder,
                                 PlatformTransactionManager transactions,
                                 @Value("${UTEFIT_QA_PASSWORD:}") String password) {
        return arguments -> {
            if (password.length() < 12) throw new IllegalStateException("Cần UTEFIT_QA_PASSWORD tối thiểu 12 ký tự cho dữ liệu QA.");
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                if (users.findByEmail("qa.admin@utefit.test").isPresent()) return;
                User admin = account(users, encoder, password, "admin", UserRole.ADMIN);
                User member = account(users, encoder, password, "member", UserRole.MEMBER);
                account(users, encoder, password, "member2", UserRole.MEMBER);
                User pt = account(users, encoder, password, "pt", UserRole.PT);
                Branch branch = branches.save(Branch.builder().name("QA UTEFit Branch")
                        .address("QA only").openingTime(LocalTime.of(6, 0))
                        .closingTime(LocalTime.of(22, 0)).active(true).build());
                branches.save(Branch.builder().name("QA Other Branch").address("QA scope test")
                        .openingTime(LocalTime.of(6, 0)).closingTime(LocalTime.of(22, 0)).active(true).build());
                plans.save(MembershipPlan.builder().name("QA 30 Days").durationDays(30)
                        .price(new BigDecimal("100000")).active(true).build());
                ClassCategory category = categories.save(ClassCategory.builder().name("Yoga")
                        .description("QA only").active(true).build());
                Trainer trainer = trainers.save(Trainer.builder().user(pt).branch(branch)
                        .specialization("Yoga").yearsExperience(3).bio("QA trainer").active(true).build());
                FitnessClass fitnessClass = classes.save(FitnessClass.builder().name("QA Yoga")
                        .branch(branch).category(category).trainer(trainer).capacity(3)
                        .status(ClassStatus.OPEN).build());
                schedules.save(ClassSchedule.builder().fitnessClass(fitnessClass).scheduleDate(LocalDate.now().plusDays(1))
                        .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).room("QA Room").active(true).build());
            });
        };
    }

    private User account(UserRepository users, PasswordEncoder encoder, String password, String label, UserRole role) {
        return users.save(User.builder().email("qa." + label + "@utefit.test")
                .firstName("QA").lastName(label).password(encoder.encode(password))
                .role(role).status(UserStatus.ACTIVE).enabled(true).tokenVersion(0).build());
    }
}
