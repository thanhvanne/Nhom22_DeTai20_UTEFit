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

    public record Checkout(Long id, String reference, String environment, String bankCode, String accountName,
                           String accountNumber, BigDecimal amount, String status, String membershipStatus,
                           long expiresAt, boolean expired, String qrUrl, boolean testMode) {}
    public record Status(String paymentStatus, String membershipStatus, boolean expired) {}

    @Transactional
    public Long start(String email, Long membershipId) {
        properties.requireConfigured();
        databaseGuard.requireSafeDatabase();
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
        if (payments.existsByMembership_IdAndStatus(membershipId, PaymentStatus.PAID)
                || memberships.existsByUser_IdAndStatusAndEndDateGreaterThanEqual(member.getId(), MembershipStatus.ACTIVE, now().toLocalDate())) {
            throw new IllegalStateException("Đơn đã có tiền được xác nhận hoặc Member đang có gói còn hiệu lực; cần đối soát.");
        }
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
                .expiresAt(now().plusMinutes(properties.getPaymentMinutes())).build());
        return payment.getId();
    }

    public Checkout checkout(String email, Long id) {
        SepayPaymentDetails order = own(email, id);
        Payment payment = order.getPayment();
        boolean expired = !order.getExpiresAt().isAfter(now());
        boolean test = order.getEnvironment().equals("TEST");
        String qr = (!test || properties.isTestQrPreview()) && !expired && payment.getStatus() == PaymentStatus.PENDING
                && configuredFor(order) && payment.getMembership().getStatus() == MembershipStatus.PENDING
                && Boolean.TRUE.equals(payment.getMembership().getBranch().getActive())
                && Boolean.TRUE.equals(payment.getMembership().getPlan().getActive())
                && !memberships.existsByUser_IdAndStatusAndEndDateGreaterThanEqual(payment.getUser().getId(), MembershipStatus.ACTIVE, now().toLocalDate())
                ? "https://vietqr.app/img?acc=" + encode(order.getAccountNumber()) + "&bank=" + encode(order.getBankCode())
                    + "&amount=" + payment.getAmount().toBigIntegerExact() + "&des=" + encode(order.getPaymentReference()) : null;
        return new Checkout(id, order.getPaymentReference(), order.getEnvironment(), order.getBankCode(), order.getAccountName(),
                order.getAccountNumber(), payment.getAmount(), payment.getStatus().name(), payment.getMembership().getStatus().name(),
                order.getExpiresAt().atZone(ZONE).toInstant().toEpochMilli(), expired, qr, test);
    }

    public Status status(String email, Long id) {
        SepayPaymentDetails order = own(email, id);
        return new Status(order.getPayment().getStatus().name(), order.getPayment().getMembership().getStatus().name(),
                !order.getExpiresAt().isAfter(now()));
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

    private boolean configuredFor(SepayPaymentDetails order) {
        try {
            properties.requireConfigured();
            return properties.mode().equals(order.getEnvironment()) && order.getAccountNumber().equals(properties.getAccountNumber())
                    && order.getBankCode().equalsIgnoreCase(properties.getBankCode()) && order.getGateway().equalsIgnoreCase(properties.getGateway());
        } catch (IllegalStateException ex) {
            return false;
        }
    }
}
