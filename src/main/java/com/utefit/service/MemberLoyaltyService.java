package com.utefit.service;

import com.utefit.config.LoyaltyProperties;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberLoyaltyService {
    private final PaymentRepository payments;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final LoyaltyPaymentExclusionRepository exclusions;
    private final SepayPaymentDetailsRepository sepayDetails;
    private final LoyaltyProperties policy;
    private final Clock clock;
    private final AutomaticPromotionService automatic;
    private static final Pattern DEMO_CASH = Pattern.compile("DEMO-CASH-M(02A|03A|04A|04NEXT|05A|05B|06A|[7-9]|1[0-9]|20)");

    public record Summary(String tier, BigDecimal spending, String nextTier, BigDecimal remaining,
                          int progress, BigDecimal percent, long usedDays, long requiredDays,
                          boolean newCustomer, boolean welcomeAvailable, boolean welcomeReserved,
                          boolean loyal, boolean enabled) {}
    public record History(Summary summary, List<Membership> paidMemberships) {}
    private record Period(LocalDate start, LocalDate end) {}

    public Summary summary(String email) { return history(member(email)).summary(); }

    public User member(String email) {
        User member = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội viên."));
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(member.getEnabled())) throw new org.springframework.security.access.AccessDeniedException("Chỉ hội viên đang hoạt động được xem ưu đãi cá nhân.");
        return member;
    }

    public History history(User member) {
        return history(member, automatic.settings());
    }

    public History history(User member, AutomaticPromotionService.Settings settings) {
        LocalDate today = LocalDate.now(clock.withZone(SepayPaymentService.ZONE));
        Set<Long> excluded = exclusions.excludedForUser(member.getId());
        Set<Long> live = sepayDetails.livePaymentIds(member.getId());
        var allPayments = payments.findByUser_IdOrderByCreatedAtDesc(member.getId());
        Set<Long> demoMemberships = allPayments.stream().filter(payment -> isSeedCash(payment) || excluded.contains(payment.getId())).filter(payment -> payment.getMembership() != null).map(payment -> payment.getMembership().getId()).collect(java.util.stream.Collectors.toSet());
        List<Payment> history = allPayments.stream()
                .filter(payment -> baseEligible(payment, member.getId(), excluded, live, today, false))
                .sorted(Comparator.comparing(Payment::getPaidAt).thenComparing(Payment::getId)).toList();
        boolean purchased = !history.isEmpty();
        List<Payment> tierHistory = allPayments.stream()
                .filter(payment -> baseEligible(payment, member.getId(), excluded, live, today, true))
                .sorted(Comparator.comparing(Payment::getPaidAt).thenComparing(Payment::getId)).toList();
        BigDecimal total = confirmedUniquePayments(tierHistory).stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Membership> paid = confirmedUniquePayments(history).stream().map(Payment::getMembership).toList();
        List<Period> periods = paid.stream().filter(membership -> membership.getStartDate() != null && membership.getEndDate() != null)
                .map(membership -> new Period(membership.getStartDate(), membership.getEndDate().isAfter(today) ? today : membership.getEndDate()))
                .filter(period -> !period.start().isAfter(period.end())).sorted(Comparator.comparing(Period::start)).toList();
        long days = unionDays(periods);
        long required = periods.isEmpty() ? 0 : ChronoUnit.DAYS.between(periods.get(0).start(), periods.get(0).start().plusMonths(settings.rule(AutomaticPromotionKind.LOYAL).minimumMonths()));
        var orders = memberships.findByUser_IdOrderByCreatedAtDesc(member.getId());
        boolean welcomeReserved = orders.stream().anyMatch(order -> "WELCOME".equals(order.getOfferKind()) && order.getStatus() == MembershipStatus.PENDING);
        boolean openOrders = orders.stream().anyMatch(order -> order.getStatus() == MembershipStatus.PENDING && !demoMemberships.contains(order.getId()));
        String tier = total.compareTo(policy.getGoldThreshold()) >= 0 ? "GOLD" : total.compareTo(policy.getSilverThreshold()) >= 0 ? "SILVER" : "BRONZE";
        BigDecimal next = tier.equals("BRONZE") ? policy.getSilverThreshold() : policy.getGoldThreshold();
        var tierRule = tier.equals("BRONZE") ? null : settings.rule(AutomaticPromotionKind.valueOf(tier));
        BigDecimal percent = tierRule != null && tierRule.available() ? tierRule.percent() : BigDecimal.ZERO;
        int progress = tier.equals("GOLD") ? 100 : total.multiply(BigDecimal.valueOf(100)).divide(next, 0, RoundingMode.DOWN).min(BigDecimal.valueOf(100)).intValue();
        return new History(new Summary(tier, total, tier.equals("GOLD") ? null : tier.equals("SILVER") ? "GOLD" : "SILVER",
                tier.equals("GOLD") ? BigDecimal.ZERO : next.subtract(total).max(BigDecimal.ZERO), progress, percent, days, required,
                !purchased, !purchased && !openOrders, welcomeReserved, required > 0 && days >= required, settings.anyActive()), List.copyOf(paid));
    }

    private boolean baseEligible(Payment payment, Long userId, Set<Long> excluded, Set<Long> live, LocalDate today, boolean forTier) {
        if ((!(forTier && payment.getMethod() == PaymentMethod.CASH) && (excluded.contains(payment.getId()) || isSeedCash(payment)))
                || payment.getPaidAt() == null || payment.getPaidAt().toLocalDate().isAfter(today)
                || payment.getAmount() == null || payment.getAmount().signum() <= 0 || payment.getMembership() == null
                || !payment.getMembership().getUser().getId().equals(userId) || (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.REFUNDED)) return false;
        return payment.getMethod() == PaymentMethod.CASH || payment.getMethod() == PaymentMethod.SEPAY && live.contains(payment.getId());
    }

    private List<Payment> confirmedUniquePayments(List<Payment> history) {
        Set<Long> countedMemberships = new HashSet<>();
        Set<String> countedTransactions = new HashSet<>();
        List<Payment> confirmed = new ArrayList<>();
        for (Payment payment : history) {
            Membership membership = payment.getMembership();
            if (payment.getStatus() != PaymentStatus.PAID || (membership.getStatus() != MembershipStatus.ACTIVE && membership.getStatus() != MembershipStatus.EXPIRED)) continue;
            String reference = payment.getGatewayTransactionNo() != null && !payment.getGatewayTransactionNo().isBlank() ? payment.getGatewayTransactionNo() : payment.getTransactionCode();
            String identity = payment.getMethod() + ":" + (reference == null || reference.isBlank() ? "ID-" + payment.getId() : reference.toUpperCase(Locale.ROOT));
            if (countedMemberships.contains(membership.getId()) || countedTransactions.contains(identity)) continue;
            countedMemberships.add(membership.getId());
            countedTransactions.add(identity);
            confirmed.add(payment);
        }
        return List.copyOf(confirmed);
    }

    public static boolean isSeedCash(Payment payment) {
        if (payment.getMethod() != PaymentMethod.CASH || payment.getTransactionCode() == null || payment.getUser() == null) return false;
        var matcher = DEMO_CASH.matcher(payment.getTransactionCode());
        if (!matcher.matches()) return false;
        int number = Integer.parseInt(matcher.group(1).replaceAll("[^0-9]", ""));
        return String.format(Locale.ROOT, "member%02d@utefit.test", number).equalsIgnoreCase(payment.getUser().getEmail());
    }

    private long unionDays(List<Period> periods) {
        long days = 0;
        LocalDate last = null;
        for (Period period : periods) {
            LocalDate start = last != null && !period.start().isAfter(last) ? last.plusDays(1) : period.start();
            if (!start.isAfter(period.end())) days += ChronoUnit.DAYS.between(start, period.end()) + 1;
            if (last == null || period.end().isAfter(last)) last = period.end();
        }
        return days;
    }
}
