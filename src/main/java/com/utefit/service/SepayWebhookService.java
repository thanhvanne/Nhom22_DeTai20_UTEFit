package com.utefit.service;

import com.utefit.config.SepayProperties;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SepayWebhookService {
    private static final Pattern REFERENCES = Pattern.compile("(?i)(?<![A-Z0-9])([A-Z]{2,5}[A-F0-9]{24})(?![A-Z0-9])");
    private final SepayProperties properties;
    private final SepayPaymentDetailsRepository details;
    private final SepayWebhookTransactionRepository transactions;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PaymentRepository payments;
    private final MembershipPaymentService billing;
    private final EntityManager entityManager;
    private final Clock clock;
    private final SepayDatabaseGuard databaseGuard;

    @Transactional(timeout = 15)
    public void accept(SepayWebhookPayload payload) {
        databaseGuard.requireSafeDatabase();
        if (duplicate(payload)) return;
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), SepayPaymentService.ZONE);
        SepayWebhookTransaction receipt = transactions.saveAndFlush(SepayWebhookTransaction.builder()
                .environment(properties.mode()).transactionId(payload.id()).gateway(payload.gateway())
                .accountNumber(payload.accountNumber()).paymentReference(payload.code()).bankReference(blankToNull(payload.referenceCode()))
                .content(payload.content()).transferType(payload.transferType()).amount(payload.amount())
                .transactionDate(payload.transactionDate()).receivedAt(now).fingerprint(payload.fingerprint()).bodyHash(payload.bodyHash())
                .outcome("REVIEW").reason("UNMATCHED").build());
        if (!payload.transferType().equals("in")) {
            receipt.setOutcome("IGNORED");
            receipt.setReason("OUTGOING_TRANSFER");
            return;
        }
        if (!payload.accountNumber().equals(properties.getAccountNumber()) || !payload.gateway().equalsIgnoreCase(properties.getGateway())) {
            receipt.setReason("WRONG_BENEFICIARY_OR_GATEWAY");
            return;
        }
        if (!hasSingleReference(payload)) {
            receipt.setReason("MISSING_OR_AMBIGUOUS_REFERENCE");
            return;
        }
        var target = details.findTargetByReference(payload.code()).orElse(null);
        if (target == null || target.getMembershipId() == null) {
            receipt.setReason("UNKNOWN_REFERENCE");
            return;
        }
        User member = users.lockByIdForBooking(target.getUserId()).orElseThrow();
        entityManager.refresh(member);
        Membership membership = memberships.lockById(target.getMembershipId()).orElseThrow();
        entityManager.refresh(membership);
        Payment payment = payments.lockById(target.getPaymentId()).orElseThrow();
        entityManager.refresh(payment);
        SepayPaymentDetails order = details.findById(payment.getId()).orElseThrow();
        receipt.setPayment(payment);
        if (payment.getMethod() != PaymentMethod.SEPAY || !order.getEnvironment().equals(properties.mode())
                || !order.getPaymentReference().equals(payload.code()) || !payload.code().equals(payment.getTransactionCode())
                || !order.getAccountNumber().equals(payload.accountNumber()) || !order.getGateway().equalsIgnoreCase(payload.gateway())
                || payment.getMembership() == null || !payment.getMembership().getId().equals(membership.getId())
                || !membership.getUser().getId().equals(member.getId()) || !payment.getUser().getId().equals(member.getId())) {
            receipt.setReason("ORDER_SNAPSHOT_MISMATCH");
            return;
        }
        if (payment.getAmount().compareTo(payload.amount()) != 0) {
            receipt.setReason("AMOUNT_MISMATCH");
            return;
        }
        if (payment.getStatus() != PaymentStatus.PENDING || membership.getStatus() != MembershipStatus.PENDING) {
            receipt.setReason("PAYMENT_OR_MEMBERSHIP_NOT_PENDING");
            return;
        }
        if (!now.isBefore(order.getExpiresAt()) || payload.transactionDate().isAfter(order.getExpiresAt())
                || payload.transactionDate().isBefore(payment.getCreatedAt().truncatedTo(ChronoUnit.SECONDS))
                || payload.transactionDate().isAfter(now.plusMinutes(1))) {
            receipt.setReason("LATE_OR_INVALID_TRANSACTION_DATE");
            return;
        }
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(member.getEnabled())
                || !Boolean.TRUE.equals(membership.getBranch().getActive()) || !Boolean.TRUE.equals(membership.getPlan().getActive())
                || membership.getQuotedPrice() == null || membership.getQuotedPrice().compareTo(payment.getAmount()) != 0
                || membership.getQuotedDurationDays() == null || membership.getQuotedDurationDays() <= 0) {
            receipt.setReason("MEMBER_OR_MEMBERSHIP_NOT_ELIGIBLE");
            return;
        }
        if (memberships.existsByUser_IdAndStatusAndEndDateGreaterThanEqual(member.getId(), MembershipStatus.ACTIVE, now.toLocalDate())) {
            receipt.setReason("ANOTHER_ACTIVE_MEMBERSHIP");
            return;
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(payload.transactionDate());
        payment.setGatewayTransactionNo(Long.toString(payload.id()));
        payment.setGatewayResponseCode("SEPAY_OK");
        billing.activateMembership(membership, payload.transactionDate());
        receipt.setOutcome("MATCHED");
        receipt.setReason("VERIFIED_AND_ACTIVATED");
        payments.flush();
    }

    @Transactional(timeout = 10)
    public boolean duplicate(SepayWebhookPayload payload) {
        databaseGuard.requireSafeDatabase();
        var existing = transactions.findByEnvironmentAndTransactionId(properties.mode(), payload.id());
        if (existing.isEmpty() && !payload.referenceCode().isBlank()) {
            existing = transactions.findByEnvironmentAndGatewayAndAccountNumberAndBankReference(properties.mode(),
                    payload.gateway(), payload.accountNumber(), payload.referenceCode());
        }
        if (existing.isEmpty()) return false;
        SepayWebhookTransaction receipt = existing.get();
        if (!receipt.getFingerprint().equals(payload.fingerprint())) {
            receipt.setConflictTransactionId(payload.id());
            receipt.setConflictHash(payload.fingerprint());
        }
        transactions.flush();
        return true;
    }

    @Transactional(readOnly = true)
    public Page<SepayWebhookTransaction> reconciliation(int page) {
        return transactions.findAllByOrderByReceivedAtDescIdDesc(PageRequest.of(Math.max(0, page), 20));
    }

    private boolean hasSingleReference(SepayWebhookPayload payload) {
        if (!payload.code().matches("[A-Z]{2,5}[A-F0-9]{24}")) return false;
        Set<String> found = new HashSet<>();
        var matcher = REFERENCES.matcher(payload.content());
        while (matcher.find()) found.add(matcher.group(1).toUpperCase(Locale.ROOT));
        return found.equals(Set.of(payload.code()));
    }

    private String blankToNull(String value) { return value.isBlank() ? null : value; }
}
