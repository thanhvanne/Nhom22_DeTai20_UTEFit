package com.utefit.service;

import com.utefit.config.SepayProperties;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SepayDatabaseGuard {
    private final SepayProperties properties;
    private final EntityManager entityManager;

    public void requireSafeDatabase() {
        properties.requireConfigured();
        if (properties.testMode()) {
            entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
                if (!properties.getTestDatabaseName().equalsIgnoreCase(connection.getCatalog())) {
                    throw new IllegalStateException("SePay Test bị chặn: SQL catalog thực tế không khớp database QA đã cấu hình.");
                }
            });
        }
    }
}
