package com.utefit.service;

import com.utefit.config.SepayProperties;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SepayDatabaseGuard implements org.springframework.boot.ApplicationRunner {
    private final SepayProperties properties;
    private final EntityManager entityManager;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void run(org.springframework.boot.ApplicationArguments arguments) {
        if (properties.isEnabled() && !properties.testMode()) requireSafeDatabase();
    }

    public void requireSafeDatabase() {
        properties.requireConfigured();
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
            String expected = properties.testMode() ? properties.getTestDatabaseName() : properties.getLiveDatabaseName();
            if (!expected.equalsIgnoreCase(connection.getCatalog())) {
                throw new IllegalStateException("SePay bị chặn: SQL catalog thực tế không khớp môi trường đã cấu hình.");
            }
        });
        if (properties.testMode()) {
            Number liveRows = (Number) entityManager.createNativeQuery("SELECT (SELECT COUNT(*) FROM dbo.sepay_payment_details WHERE environment='LIVE') + (SELECT COUNT(*) FROM dbo.sepay_webhook_transactions WHERE environment='LIVE')").getSingleResult();
            if (liveRows.longValue() != 0) throw new IllegalStateException("Test database chứa dữ liệu SePay Live; bị chặn để bảo vệ dữ liệu.");
        } else {
            var markers = entityManager.createNativeQuery("SELECT deployment_id FROM dbo.sepay_deployment_guard WHERE id=1 AND environment='LIVE'", String.class)
                    .getResultList();
            if (markers.size() != 1 || !properties.getLiveDeploymentId().equals(markers.get(0))) {
                throw new IllegalStateException("SePay Live bị chặn: database chưa được phê duyệt cho deployment này.");
            }
            Number testRows = (Number) entityManager.createNativeQuery("SELECT (SELECT COUNT(*) FROM dbo.sepay_payment_details WHERE environment='TEST') + (SELECT COUNT(*) FROM dbo.sepay_webhook_transactions WHERE environment='TEST')").getSingleResult();
            if (testRows.longValue() != 0) throw new IllegalStateException("Live database chứa dữ liệu SePay Test; cần đối soát trước khi triển khai.");
        }
    }
}
