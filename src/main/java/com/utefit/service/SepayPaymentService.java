package com.utefit.service;

import com.utefit.config.SepayProperties;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SepayPaymentService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SepayProperties properties;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PaymentRepository payments;
    private final SepayPaymentDetailsRepository details;
    private final EntityManager entityManager;
    private final Clock clock;
    private final SepayDatabaseGuard databaseGuard;
    private final SepayWebhookTransactionRepository transactions;
    private final BranchRepository branches;

    public record Checkout(Long id, String reference, String environment, String bankCode, String accountName,
                           String accountNumber, BigDecimal amount, String status, String membershipStatus,
                           long expiresAt, boolean expired, String qrUrl, boolean testMode,
                           boolean virtualAccount, boolean legacyTransferBlocked, String displayState,
                           MembershipSummary membership) {}
    public record MembershipSummary(String planName, String branchName, String benefit, String scope,
                                    LocalDate startDate, LocalDate endDate, Integer durationDays,
                                    boolean renewal, String periodState, String durationDescription) {}
    public record Status(String paymentStatus, String membershipStatus, boolean expired, boolean reconciliationRequired,
                         String displayState, String periodState) {}

    @Transactional
    public Long start(String email, Long membershipId) {
        properties.requireConfigured();
        databaseGuard.requireSafeDatabase();
        if (!properties.acceptingNewPayments()) {
            throw new IllegalStateException("Đang tạm ngừng tạo đơn SePay mới. Đơn cũ vẫn được đối soát; không chuyển tiền cho đơn mới.");
        }
        User member = users.lockByEmailForBooking(email).orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
        entityManager.refresh(member);
        requireMember(member);
        Membership membership = memberships.lockById(membershipId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói đăng ký."));
        entityManager.refresh(membership);
        if (!membership.getUser().getId().equals(member.getId())) throw new AccessDeniedException("Không sở hữu gói đăng ký.");
        if (membership.getStatus() != MembershipStatus.PENDING || !Boolean.TRUE.equals(membership.getPlan().getActive())
                || !Boolean.TRUE.equals(membership.getBranch().getActive())) {
            throw new IllegalStateException("Gói không còn chờ thanh toán hoặc đã ngừng hoạt động.");
        }
        if (payments.existsByMembership_IdAndStatus(membershipId, PaymentStatus.PAID)) {
            throw new IllegalStateException("Đơn đã có tiền được xác nhận; cần đối soát.");
        }
        if (payments.existsByMembership_IdAndStatus(membershipId, PaymentStatus.CANCEL_REQUESTED))
            throw new IllegalStateException("Đơn đang yêu cầu hủy; không được mở lại thanh toán.");
        var pending = payments.findByMembership_IdAndStatus(membershipId, PaymentStatus.PENDING);
        if (!pending.isEmpty()) {
            if (pending.size() != 1 || pending.get(0).getMethod() != PaymentMethod.SEPAY) {
                throw new IllegalStateException("Đã có giao dịch CASH/VNPay đang xử lý. Không thể chuyển phương thức.");
            }
            SepayPaymentDetails existing = details.findById(pending.get(0).getId()).orElseThrow();
            if (!existing.getEnvironment().equals(properties.mode())) throw new IllegalStateException("Giao dịch thuộc môi trường khác; cần đối soát.");
            return existing.getId();
        }
        BigDecimal price = membership.getQuotedPrice();
        if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 0
                || membership.getQuotedDurationDays() == null || membership.getQuotedDurationDays() <= 0) {
            throw new IllegalStateException("Gói cũ thiếu snapshot giá/thời hạn hợp lệ. Cần Admin đối soát, không dùng giá hiện tại thay thế.");
        }
        byte[] entropy = new byte[12];
        RANDOM.nextBytes(entropy);
        String reference = properties.getPaymentPrefix() + HexFormat.of().withUpperCase().formatHex(entropy);
        Payment payment = payments.saveAndFlush(Payment.builder().user(member).membership(membership).amount(price)
                .method(PaymentMethod.SEPAY).status(PaymentStatus.PENDING).transactionCode(reference).createdAt(now()).build());
        details.saveAndFlush(SepayPaymentDetails.builder().payment(payment).paymentReference(reference)
                .environment(properties.mode()).bankCode(properties.getBankCode()).gateway(properties.getGateway().toUpperCase(java.util.Locale.ROOT))
                .accountNumber(properties.getAccountNumber()).accountName(properties.getAccountName())
                .vaNumber(blankToNull(properties.getVaNumber()))
                .expiresAt(now().plusMinutes(properties.getPaymentMinutes())).build());
        return payment.getId();
    }

    public Checkout checkout(String email, Long id) {
        SepayPaymentDetails order = own(email, id);
        Payment payment = order.getPayment();
        boolean expired = !order.getExpiresAt().isAfter(now());
        boolean test = order.getEnvironment().equals("TEST");
        boolean review = transactions.existsByPayment_IdAndOutcome(id, "REVIEW");
        String va = normalized(order.getVaNumber());
        boolean legacyBlocked = !test && va.isEmpty() && ("BIDV".equalsIgnoreCase(order.getBankCode())
                || "BIDV".equalsIgnoreCase(order.getGateway()) || !normalized(properties.getVaNumber()).isEmpty());
        String receivingAccount = legacyBlocked ? null : va.isEmpty() ? order.getAccountNumber() : va;
        String qr = (!test || properties.isTestQrPreview()) && !expired && payment.getStatus() == PaymentStatus.PENDING
                && !legacyBlocked && configuredFor(order) && !review
                && payment.getMembership().getStatus() == MembershipStatus.PENDING
                && Boolean.TRUE.equals(payment.getMembership().getBranch().getActive())
                && Boolean.TRUE.equals(payment.getMembership().getPlan().getActive())
                ? "https://vietqr.app/img?acc=" + encode(receivingAccount) + "&bank=" + encode(order.getBankCode())
                    + "&amount=" + payment.getAmount().toBigIntegerExact() + "&des=" + encode(order.getPaymentReference()) : null;
        return new Checkout(id, order.getPaymentReference(), order.getEnvironment(), order.getBankCode(), order.getAccountName(),
                receivingAccount, payment.getAmount(), payment.getStatus().name(), payment.getMembership().getStatus().name(),
                order.getExpiresAt().atZone(ZONE).toInstant().toEpochMilli(), expired, qr, test, !va.isEmpty(), legacyBlocked,
                displayState(payment, expired, review), summary(payment.getMembership()));
    }

    public Status status(String email, Long id) {
        SepayPaymentDetails order = own(email, id);
        boolean expired = !order.getExpiresAt().isAfter(now());
        boolean review = transactions.existsByPayment_IdAndOutcome(id, "REVIEW");
        return new Status(order.getPayment().getStatus().name(), order.getPayment().getMembership().getStatus().name(),
                expired, review, displayState(order.getPayment(), expired, review), periodState(order.getPayment().getMembership()));
    }

    private String displayState(Payment payment, boolean expired, boolean review) {
        if (review && (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.CANCEL_REQUESTED)) return "REVIEW";
        if (payment.getStatus() != PaymentStatus.PENDING) return payment.getStatus().name();
        return review ? "REVIEW" : expired ? "EXPIRED" : "PENDING";
    }

    private String periodState(Membership membership) {
        LocalDate today = now().toLocalDate();
        if (membership.getStatus() != MembershipStatus.ACTIVE) return membership.getStatus().name();
        if (membership.getStartDate() == null || membership.getEndDate() == null) return "UNKNOWN";
        if (membership.getStartDate().isAfter(today)) return "FUTURE";
        return membership.getEndDate().isBefore(today) ? "EXPIRED" : "CURRENT";
    }

    private MembershipSummary summary(Membership membership) {
        String benefit = switch (membership.effectiveBenefit()) {
            case ALL -> "Gym, Yoga và PT";
            case GYM -> "Gym";
            case YOGA -> "Yoga";
            case PT -> "PT";
        };
        String scope = switch (membership.effectiveScope()) {
            case SINGLE_BRANCH -> "Một chi nhánh: " + membership.getBranch().getName();
            case ALL_BRANCHES -> "Toàn bộ chi nhánh đang hoạt động";
            case MULTI_BRANCH -> "Các chi nhánh đã chọn: " + branches.findAllById(membership.getBranchIds()).stream()
                    .map(Branch::getName).sorted().collect(java.util.stream.Collectors.joining(", "));
        };
        return new MembershipSummary(membership.getPlan().getName(), membership.getBranch().getName(), benefit, scope,
                membership.getStartDate(), membership.getEndDate(), membership.getQuotedDurationDays(),
                membership.getRenewalOf() != null, periodState(membership), membership.term().description());
    }

    private SepayPaymentDetails own(String email, Long id) {
        User member = users.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
        requireMember(member);
        SepayPaymentDetails order = details.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thanh toán VietQR."));
        if (!order.getPayment().getUser().getId().equals(member.getId())) throw new AccessDeniedException("Không được xem giao dịch của người khác.");
        return order;
    }

    private void requireMember(User member) {
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(member.getEnabled())) {
            throw new AccessDeniedException("Chỉ Member đang hoạt động được sử dụng thanh toán.");
        }
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZONE); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String normalized(String value) { return value == null ? "" : value; }
    private String blankToNull(String value) { return value == null || value.isEmpty() ? null : value; }

    private boolean configuredFor(SepayPaymentDetails order) {
        try {
            databaseGuard.requireSafeDatabase();
            return properties.mode().equals(order.getEnvironment()) && order.getAccountNumber().equals(properties.getAccountNumber())
                    && normalized(order.getVaNumber()).equals(normalized(properties.getVaNumber()))
                    && order.getBankCode().equalsIgnoreCase(properties.getBankCode()) && order.getGateway().equalsIgnoreCase(properties.getGateway());
        } catch (IllegalStateException ex) {
            return false;
        }
    }
}
