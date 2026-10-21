
package com.utefit.service;

import com.utefit.enums.MembershipStatus;
import com.utefit.enums.PaymentStatus;
import com.utefit.enums.UserRole;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final EntityManager entityManager;

    private long count(String jpql) {
        return entityManager
                .createQuery(jpql, Long.class)
                .getSingleResult();
    }

    private BigDecimal paidRevenue(
            LocalDateTime from,
            LocalDateTime to
    ) {
        return entityManager.createQuery("""
                SELECT SUM(p.amount)
                FROM Payment p
                WHERE p.status = :status
                  AND p.paidAt >= :from
                  AND p.paidAt < :to
                """, BigDecimal.class)
                .setParameter("status", PaymentStatus.PAID)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultStream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> report() {

        Map<String, Object> data = new HashMap<>();

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart =
                currentMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart =
                currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        long totalMembers = entityManager.createQuery("""
                SELECT COUNT(u) FROM User u
                WHERE u.role = :role
                """, Long.class)
                .setParameter("role", UserRole.MEMBER)
                .getSingleResult();

        long totalTrainers = count(
                "SELECT COUNT(t) FROM Trainer t");

        long totalBranches = count(
                "SELECT COUNT(b) FROM Branch b");

        long totalClasses = count(
                "SELECT COUNT(c) FROM FitnessClass c");

        long activeMemberships = entityManager.createQuery("""
                SELECT COUNT(m) FROM Membership m
                WHERE m.status = :status
                  AND m.startDate <= :today
                  AND m.endDate >= :today
                """, Long.class)
                .setParameter("status", MembershipStatus.ACTIVE)
                .setParameter("today", today)
                .getSingleResult();

        long checkinsThisMonth = entityManager.createQuery("""
                SELECT COUNT(c) FROM Checkin c
                WHERE c.checkInAt >= :from
                  AND c.checkInAt < :to
                """, Long.class)
                .setParameter("from", monthStart)
                .setParameter("to", nextMonthStart)
                .getSingleResult();

        long newMembersThisMonth = entityManager.createQuery("""
                SELECT COUNT(u) FROM User u
                WHERE u.role = :role
                  AND u.createdAt >= :from
                  AND u.createdAt < :to
                """, Long.class)
                .setParameter("role", UserRole.MEMBER)
                .setParameter("from", monthStart)
                .setParameter("to", nextMonthStart)
                .getSingleResult();

        BigDecimal revenueTotal =
                entityManager.createQuery("""
                        SELECT SUM(p.amount)
                        FROM Payment p
                        WHERE p.status = :status
                        """, BigDecimal.class)
                        .setParameter("status", PaymentStatus.PAID)
                        .getResultStream()
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(BigDecimal.ZERO);

        data.put("totalMembers", totalMembers);
        data.put("totalTrainers", totalTrainers);
        data.put("totalBranches", totalBranches);
        data.put("totalClasses", totalClasses);
        data.put("activeMemberships", activeMemberships);
        data.put("checkinsThisMonth", checkinsThisMonth);
        data.put("newMembersThisMonth", newMembersThisMonth);
        data.put("revenueTotal", revenueTotal);

        // DOANH THU 6 THANG GAN NHAT.
        List<String> monthlyLabels = new ArrayList<>();
        List<BigDecimal> monthlyRevenue = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);

            LocalDateTime from = month.atDay(1).atStartOfDay();
            LocalDateTime to =
                    month.plusMonths(1).atDay(1).atStartOfDay();

            monthlyLabels.add(
                    month.format(DateTimeFormatter.ofPattern("MM/yyyy")));

            monthlyRevenue.add(paidRevenue(from, to));
        }

        data.put("monthlyLabels", monthlyLabels);
        data.put("monthlyRevenue", monthlyRevenue);

        // TOP GOI TAP THEO DOANH THU DA THANH TOAN.
        List<Object[]> topPlans = entityManager.createQuery("""
                SELECT m.plan.name, SUM(p.amount)
                FROM Payment p
                JOIN p.membership m
                WHERE p.status = :status
                GROUP BY m.plan.id, m.plan.name
                ORDER BY SUM(p.amount) DESC
                """, Object[].class)
                .setParameter("status", PaymentStatus.PAID)
                .setMaxResults(5)
                .getResultList();

        data.put("topPlanLabels",
                topPlans.stream().map(r -> (String) r[0]).toList());
        data.put("topPlanValues",
                topPlans.stream().map(r -> (BigDecimal) r[1]).toList());

        // TOP CHI NHANH THEO DOANH THU DA THANH TOAN.
        List<Object[]> topBranches = entityManager.createQuery("""
                SELECT m.branch.name, SUM(p.amount)
                FROM Payment p
                JOIN p.membership m
                WHERE p.status = :status
                GROUP BY m.branch.id, m.branch.name
                ORDER BY SUM(p.amount) DESC
                """, Object[].class)
                .setParameter("status", PaymentStatus.PAID)
                .setMaxResults(5)
                .getResultList();

        data.put("topBranchLabels",
                topBranches.stream().map(r -> (String) r[0]).toList());
        data.put("topBranchValues",
                topBranches.stream().map(r -> (BigDecimal) r[1]).toList());

        return data;
    }
}
