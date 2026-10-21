
package com.utefit.service;

import com.utefit.entity.Payment;
import com.utefit.enums.PaymentMethod;
import com.utefit.enums.PaymentStatus;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final EntityManager entityManager;

    @Getter
    @AllArgsConstructor
    public static class PaymentRow {
        private Long id;
        private String memberEmail;
        private String planName;
        private String branchName;
        private BigDecimal amount;
        private PaymentMethod method;
        private PaymentStatus status;
        private String transactionCode;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
    }

    private PaymentRow toRow(Payment p) {
        return new PaymentRow(
                p.getId(),
                p.getUser().getEmail(),
                p.getMembership() == null ? "-" :
                        p.getMembership().getPlan().getName(),
                p.getMembership() == null ? "-" :
                        p.getMembership().getBranch().getName(),
                p.getAmount(),
                p.getMethod(),
                p.getStatus(),
                p.getTransactionCode(),
                p.getCreatedAt(),
                p.getPaidAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<PaymentRow> search(
            String keyword,
            PaymentMethod method,
            PaymentStatus status,
            int page
    ) {
        String pattern = keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.trim().toLowerCase() + "%";

        String where = """
                WHERE (:keyword IS NULL
                       OR LOWER(p.user.email) LIKE :keyword)
                  AND (:method IS NULL OR p.method = :method)
                  AND (:status IS NULL OR p.status = :status)
                """;

        var query = entityManager.createQuery(
                "SELECT p FROM Payment p " + where
                        + " ORDER BY p.createdAt DESC, p.id DESC",
                Payment.class
        );

        var countQuery = entityManager.createQuery(
                "SELECT COUNT(p) FROM Payment p " + where,
                Long.class
        );

        query.setParameter("keyword", pattern);
        query.setParameter("method", method);
        query.setParameter("status", status);

        countQuery.setParameter("keyword", pattern);
        countQuery.setParameter("method", method);
        countQuery.setParameter("status", status);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                10
        );

        List<PaymentRow> rows = query
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList()
                .stream()
                .map(this::toRow)
                .toList();

        long total = countQuery.getSingleResult();

        return new PageImpl<>(rows, pageable, total);
    }

    @Transactional(readOnly = true)
    public PaymentRow detail(Long id) {
        Payment payment = entityManager.find(Payment.class, id);

        if (payment == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy giao dịch.");
        }

        return toRow(payment);
    }
}
