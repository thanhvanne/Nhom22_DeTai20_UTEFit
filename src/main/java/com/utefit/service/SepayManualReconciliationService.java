package com.utefit.service;

import com.utefit.config.SepayProperties;
import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SepayManualReconciliationService {
    public static final String METHOD = "MANUAL_BANK_RECONCILIATION";
    private final SepayProperties properties;
    private final SepayDatabaseGuard guard;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PaymentRepository payments;
    private final SepayPaymentDetailsRepository details;
    private final SepayWebhookTransactionRepository receipts;
    private final SepayBankReconciliationRepository records;
    private final MembershipPaymentService billing;
    private final EntityManager entityManager;
    private final Clock clock;
    private final MembershipBookingPolicyService bookingPolicy;
    @Value("${SEPAY_MANUAL_RECONCILIATION_ENABLED:false}") private boolean enabled;

    public record ReceiptEvidence(String gateway, String accountNumber, String subAccount, String paymentReference,
                                  BigDecimal amount, String bankReference,
                                  @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime bankDate,
                                  String source, String evidence, String reason, boolean independentlyVerified) {}
    public record RefundEvidence(String bankReference, BigDecimal amount,
                                 @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime bankDate,
                                 String source, String evidence, String recipientVerification, String reason,
                                 boolean independentlyVerified) {}
    public record CaseView(SepayPaymentDetails order, List<SepayBankReconciliation> records,
                           List<SepayWebhookTransaction> webhooks) {}

    @Transactional(readOnly = true)
    public Page<SepayPaymentDetails> pending(int page) {
        requireAdmin(false);
        return details.findReconciliationCases(PageRequest.of(Math.max(0, page), 20));
    }

    @Transactional(readOnly = true)
    public CaseView view(Long paymentId) {
        requireAdmin(false);
        var order = details.findById(paymentId).orElseThrow(() -> invalid("Không có snapshot SePay."));
        return new CaseView(order, records.findByPayment_IdOrderByIdDesc(paymentId),
                receipts.findTop20ByPayment_IdOrderByReceivedAtDesc(paymentId));
    }

    @Transactional(timeout = 20)
    public void confirmReceived(Long paymentId, String bankReference, boolean independentlyVerified) {
        requireAdmin(true);
        guard.requireSafeDatabase();
        if (!independentlyVerified)
            throw invalid("Phải xác nhận đã kiểm tra giao dịch thực nhận tại ngân hàng, đúng số tiền, tài khoản và mã đơn.");
        String reference = canonical(bankReference);
        var order = lockOrder(paymentId);
        Payment payment = order.getPayment();
        requirePending(payment);
        var existing = records.findByPayment_IdOrderByIdDesc(paymentId);
        if (!existing.isEmpty()) {
            var record = existing.stream().filter(candidate -> METHOD.equals(candidate.getMethod())
                    && "VERIFIED".equals(candidate.getOutcome()) && reference.equals(candidate.getBankReference()))
                    .findFirst().orElseThrow(() -> invalid("Đơn đã có bản ghi ngân hàng khác hoặc đã được xử lý. Kiểm tra lịch sử đối soát."));
            if (record.getAmount().compareTo(payment.getAmount()) != 0)
                throw invalid("Số tiền bằng chứng đã lưu không khớp đơn hàng.");
            activate(paymentId, record.getId());
            return;
        }
        var receipt = receipts.findBankReference("LIVE", order.getGateway().toUpperCase(Locale.ROOT),
                order.getAccountNumber(), reference).orElse(null);
        LocalDateTime confirmedAt = now();
        if (receipt == null && !confirmedAt.isBefore(order.getExpiresAt()))
            throw invalid("Đơn đã hết hạn và chưa có bằng chứng thời điểm nhận tiền. Không thể suy ra giờ ngân hàng từ snapshot. Mở xử lý riêng để đối chiếu sao kê/thời gian thực nhận; không tự kích hoạt.");
        LocalDateTime recognitionTime = receipt == null ? confirmedAt : receipt.getTransactionDate();
        String timeBasis = receipt == null
                ? "TIME_BASIS=ADMIN_CONFIRMATION; thời điểm ghi nhận là lúc Admin xác nhận, không phải giờ hạch toán ngân hàng."
                : "TIME_BASIS=AUTHENTICATED_WEBHOOK; thời điểm nhận tiền lấy từ webhook đã xác thực lưu trong hệ thống.";
        var input = new ReceiptEvidence(order.getGateway(), order.getAccountNumber(), blank(order.getVaNumber()),
                order.getPaymentReference(), payment.getAmount(), reference, recognitionTime, "RECEIVING_BANK_PORTAL",
                "Admin xác nhận đã đối chiếu trực tiếp ngân hàng nhận: đúng số tiền, tài khoản chính/VA và mã đơn "
                        + order.getPaymentReference() + ", mã ngân hàng " + reference + ". " + timeBasis,
                "Xác nhận ngắn sau khi kiểm tra tiền thực nhận; thông tin đơn lấy từ snapshot backend. " + timeBasis, true);
        Long recordId = verifyReceipt(paymentId, input);
        activate(paymentId, recordId);
    }

    @Transactional(timeout = 20)
    public Long verifyReceipt(Long paymentId, ReceiptEvidence input) {
        User actor = requireAdmin(true);
        guard.requireSafeDatabase();
        var order = lockOrder(paymentId);
        var payment = order.getPayment();
        if (!((payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.CANCEL_REQUESTED)
                && payment.getMembership().getStatus() == MembershipStatus.PENDING)
                && !(payment.getStatus() == PaymentStatus.CANCELLED && payment.getMembership().getStatus() == MembershipStatus.CANCELLED))
            throw invalid("Chỉ ghi bằng chứng cho đơn chờ hoặc đã hủy chưa hoàn tiền; không kích hoạt đơn hủy.");
        requireEvidence(input.source(), input.evidence(), input.independentlyVerified());
        requireText(input.reason(), 20, 1000, "Lý do đối soát (20–1000 ký tự)");
        String reference = canonical(input.bankReference());
        if (!order.getGateway().equalsIgnoreCase(input.gateway())
                || !order.getAccountNumber().equals(input.accountNumber())
                || !Objects.equals(blank(order.getVaNumber()), blank(input.subAccount()))
                || !order.getPaymentReference().equals(input.paymentReference())
                || !order.getPaymentReference().equals(payment.getTransactionCode())
                || input.amount() == null || payment.getAmount().compareTo(input.amount()) != 0) {
            throw invalid("Ngân hàng, tài khoản chính/VA, mã đơn hoặc số tiền không khớp snapshot.");
        }
        LocalDateTime now = now();
        if (input.bankDate() == null || input.bankDate().isAfter(now)
                || input.bankDate().isBefore(payment.getCreatedAt().truncatedTo(ChronoUnit.SECONDS))) {
            throw invalid("Thời gian nhận tiền không hợp lệ.");
        }
        if (!records.findByPayment_IdOrderByIdDesc(paymentId).isEmpty())
            throw invalid("Đơn đã có bản ghi nhận tiền. Mở bản ghi hiện có, không xác minh lại.");
        String gateway = order.getGateway().toUpperCase(Locale.ROOT);
        if (records.findByEnvironmentAndGatewayAndAccountNumberAndBankReference("LIVE", gateway,
                order.getAccountNumber(), reference).isPresent()) throw invalid("Mã ngân hàng đã được sử dụng.");
        var prior = receipts.findBankReference("LIVE", gateway, order.getAccountNumber(), reference).orElse(null);
        if (prior != null && ("MATCHED".equals(prior.getOutcome()) || prior.getPayment() == null
                || !prior.getPayment().getId().equals(paymentId) || prior.getAmount().compareTo(input.amount()) != 0
                || !"in".equals(prior.getTransferType())
                || !blank(prior.getSubAccount()).equals(blank(input.subAccount()))
                || !order.getPaymentReference().equals(prior.getPaymentReference()) || prior.getConflictHash() != null)) {
            throw invalid("Mã ngân hàng xung đột với lịch sử webhook; cần điều tra, không được gán tiền.");
        }
        return records.saveAndFlush(SepayBankReconciliation.builder().payment(payment).environment("LIVE")
                .gateway(gateway).accountNumber(order.getAccountNumber()).subAccount(blank(input.subAccount()))
                .bankReference(reference).amount(payment.getAmount()).bankDate(input.bankDate()).method(METHOD)
                .outcome("VERIFIED").verifiedBy(actor).verifiedAt(now).evidence(input.source() + ": " + input.evidence())
                .reason(input.reason()).build()).getId();
    }

    @Transactional(timeout = 20)
    public void activate(Long paymentId, Long recordId) {
        User actor = requireAdmin(true);
        guard.requireSafeDatabase();
        var order = lockOrder(paymentId);
        Payment payment = order.getPayment();
        var record = verifiedRecord(payment, recordId);
        requirePending(payment);
        Membership membership = payment.getMembership();
        User member = payment.getUser();
        if (!record.getBankDate().isBefore(order.getExpiresAt()))
            throw invalid("Tiền đến sau hạn đơn: không kích hoạt. Xác minh hoàn tiền thực tế hoặc xử lý hỗ trợ riêng.");
        if (receipts.countOtherReviews(paymentId, record.getBankReference()) > 0)
            throw invalid("Còn webhook REVIEW khác/xung đột. Không kích hoạt khi chưa làm rõ tất cả tiền nhận.");
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(member.getEnabled()) || !Boolean.TRUE.equals(membership.getBranch().getActive())
                || !Boolean.TRUE.equals(membership.getPlan().getActive()) || membership.getQuotedPrice() == null
                || membership.getQuotedPrice().compareTo(payment.getAmount()) != 0
                || membership.getQuotedDurationDays() == null || membership.getQuotedDurationDays() <= 0)
            throw invalid("Hội viên/gói/chi nhánh/snapshot không đủ điều kiện kích hoạt.");
        if (membership.term().endDate(billing.activationStart(membership, record.getBankDate().toLocalDate())).isBefore(now().toLocalDate()))
            throw invalid("Quyền lợi tính theo ngày nhận tiền đã hết hạn. Cần xử lý hỗ trợ/hoàn tiền, không tự đổi snapshot.");
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(record.getBankDate());
        payment.setGatewayTransactionNo(record.getBankReference());
        payment.setGatewayResponseCode("MANUAL_OK");
        billing.activateMembership(membership, record.getBankDate());
        record.setOutcome("APPLIED");
        record.setResolvedBy(actor);
        record.setResolvedAt(now());
        payments.flush();
    }

    @Transactional(timeout = 20)
    public void confirmRefund(Long paymentId, Long recordId, RefundEvidence input) {
        User actor = requireAdmin(true);
        guard.requireSafeDatabase();
        var order = lockOrder(paymentId);
        Payment payment = order.getPayment();
        var record = records.findById(recordId).orElseThrow(() -> invalid("Không có bằng chứng nhận tiền."));
        entityManager.refresh(record);
        boolean pendingRefund = ((payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.CANCEL_REQUESTED)
                && payment.getMembership().getStatus() == MembershipStatus.PENDING
                || payment.getStatus() == PaymentStatus.CANCELLED && payment.getMembership().getStatus() == MembershipStatus.CANCELLED)
                && METHOD.equals(record.getMethod()) && "VERIFIED".equals(record.getOutcome());
        boolean paidRefund = payment.getStatus() == PaymentStatus.PAID
                && (payment.getMembership().getStatus() == MembershipStatus.ACTIVE || payment.getMembership().getStatus() == MembershipStatus.EXPIRED)
                && ("APPLIED".equals(record.getOutcome()) || "WEBHOOK".equals(record.getOutcome()));
        if (!record.getPayment().getId().equals(paymentId) || (!pendingRefund && !paidRefund)
                || record.getAmount().compareTo(payment.getAmount()) != 0)
            throw invalid("Khoản nhận tiền không thuộc đơn, đã hoàn/hủy hoặc trạng thái không cho phép hoàn tiền.");
        requireEvidence(input.source(), input.evidence(), input.independentlyVerified());
        requireText(input.recipientVerification(), 20, 500, "Đối chiếu người nhận hoàn tiền với người chuyển gốc");
        requireText(input.reason(), 20, 500, "Lý do hoàn tiền/hủy đơn (20–500 ký tự)");
        if (receipts.countOtherReviews(paymentId, record.getBankReference()) > 0)
            throw invalid("Còn giao dịch REVIEW khác/xung đột; không hủy đơn khi chưa làm rõ các khoản tiền.");
        String refundReference = canonical(input.bankReference());
        if (input.amount() == null || record.getAmount().compareTo(input.amount()) != 0
                || input.bankDate() == null || input.bankDate().isBefore(record.getBankDate()) || input.bankDate().isAfter(now())
                || refundReference.equals(record.getBankReference()) || records.existsByRefundReference(refundReference))
            throw invalid("Hoàn tiền phải đủ số tiền, đúng thời gian và có mã giao dịch riêng chưa sử dụng.");
        record.setOutcome("REFUNDED");
        record.setRefundReference(refundReference);
        record.setRefundedAt(input.bankDate());
        record.setResolutionEvidence((paidRefund ? "Trước hoàn: " + record.getMethod() + "; Admin kích hoạt ID="
                + (record.getResolvedBy() == null ? "SYSTEM" : record.getResolvedBy().getId()) + "; lúc=" + record.getResolvedAt() + ". " : "")
                + input.source() + ": " + input.evidence() + " | " + input.recipientVerification() + " | Lý do: " + input.reason());
        record.setResolvedBy(actor);
        record.setResolvedAt(now());
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.getMembership().setStatus(MembershipStatus.CANCELLED);
        records.flush();
        payments.flush();
        bookingPolicy.revalidateMember(payment.getUser().getId());
    }

    private User requireAdmin(boolean mutation) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) throw new AccessDeniedException("Chỉ Admin được đối soát.");
        User actor = users.findByEmail(auth.getName()).orElseThrow(() -> new AccessDeniedException("Admin không tồn tại."));
        if (mutation) entityManager.refresh(actor);
        if (actor.getRole() != UserRole.ADMIN || actor.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(actor.getEnabled()))
            throw new AccessDeniedException("Admin không còn quyền hoạt động.");
        if (mutation && (!enabled || !"LIVE".equals(properties.mode())))
            throw new AccessDeniedException("Đối soát thủ công chưa bật hoặc không phải môi trường Live.");
        return actor;
    }

    private SepayPaymentDetails lockOrder(Long id) {
        var target = details.findById(id).orElseThrow(() -> invalid("Không có đơn SePay."));
        var original = target.getPayment();
        if (original.getMembership() == null) throw invalid("Đơn không có Membership.");
        var member = users.lockByIdForBooking(original.getUser().getId()).orElseThrow();
        entityManager.refresh(member);
        var membership = memberships.lockById(original.getMembership().getId()).orElseThrow();
        entityManager.refresh(membership);
        var payment = payments.lockById(id).orElseThrow();
        entityManager.refresh(payment);
        if (!"LIVE".equals(target.getEnvironment()) || payment.getMethod() != PaymentMethod.SEPAY
                || payment.getMembership() == null || !payment.getMembership().getId().equals(membership.getId())
                || !payment.getUser().getId().equals(member.getId())
                || !payment.getUser().getId().equals(membership.getUser().getId())) throw invalid("Sai môi trường/phương thức/chủ đơn.");
        return target;
    }

    private SepayBankReconciliation verifiedRecord(Payment payment, Long id) {
        var record = records.findById(id).orElseThrow(() -> invalid("Không có bằng chứng nhận tiền."));
        entityManager.refresh(record);
        if (!record.getPayment().getId().equals(payment.getId()) || !METHOD.equals(record.getMethod())
                || !"VERIFIED".equals(record.getOutcome())) throw invalid("Bằng chứng không thuộc đơn hoặc đã được xử lý.");
        return record;
    }

    private void requirePending(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING || payment.getMembership().getStatus() != MembershipStatus.PENDING)
            throw invalid("Chỉ xử lý Payment và Membership đang PENDING.");
    }
    private void requireEvidence(String source, String evidence, boolean verified) {
        if (!verified || !("RECEIVING_BANK_PORTAL".equals(source) || "OFFICIAL_BANK_STATEMENT".equals(source)))
            throw invalid("Phải đối chiếu trực tiếp ngân hàng nhận/sao kê chính thức; ảnh người chuyển không đủ.");
        requireText(evidence, 30, 1500, "Bằng chứng ngân hàng (30–1500 ký tự, mã sao kê/tài liệu lưu nội bộ)");
    }
    private void requireText(String text, int min, int max, String message) {
        if (text == null || text.strip().length() < min || text.length() > max) throw invalid(message);
    }
    private String canonical(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._/-]{2,99}"))
            throw invalid("Cần mã giao dịch duy nhất do ngân hàng cấp (3–100 ký tự, không có khoảng trắng).");
        return value.toUpperCase(Locale.ROOT);
    }
    private String blank(String value) { return value == null ? "" : value; }
    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), SepayPaymentService.ZONE); }
    private IllegalStateException invalid(String message) { return new IllegalStateException(message); }
}
