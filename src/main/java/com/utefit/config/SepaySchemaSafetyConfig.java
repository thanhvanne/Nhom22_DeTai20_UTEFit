package com.utefit.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import java.util.Arrays;
import java.util.Locale;

@Configuration
public class SepaySchemaSafetyConfig {
    @Bean
    static BeanFactoryPostProcessor requireManualSqlServerMigration(Environment environment) {
        return factory -> {
            String url = environment.getProperty("spring.datasource.url", "");
            String ddl = environment.getProperty("spring.jpa.hibernate.ddl-auto", "none").toLowerCase(Locale.ROOT);
            boolean isolatedQa = Arrays.asList(environment.getActiveProfiles()).contains("qa")
                    && url.contains("databaseName=UTEFitQA_GD7_20261025;");
            if (url.startsWith("jdbc:sqlserver:") && !isolatedQa && !ddl.equals("validate") && !ddl.equals("none")) {
                throw new IllegalStateException("SePay cần migration V706 thủ công. Chạy profile sepay (8080, ddl-auto=validate); không tự update/create schema SQL Server chính.");
            }
        };
    }
}
