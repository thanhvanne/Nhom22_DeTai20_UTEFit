package com.utefit.service;

import com.utefit.config.VnpayProperties;
import com.utefit.entity.Membership;
import com.utefit.entity.Payment;
import com.utefit.entity.User;
import com.utefit.enums.*;
import com.utefit.repository.MembershipRepository;
import com.utefit.repository.PaymentRepository;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VnpayPaymentService {
    private final VnpayProperties properties;
    private final VnpayGateway gateway;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PaymentRepository payments;
    private final MembershipPaymentService billing;

    @Transactional
    public String start(String email, Long membershipId, String ipAddress) {
        properties.requireConfigured();
        User member = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(member.getEnabled())) throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        Membership membership = memberships.lockById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói đăng ký."));
        if (!membership.getUser().getId().equals(member.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Không sở hữu gói đăng ký.");
        }
        if (membership.getStatus() != MembershipStatus.PENDING
                || !Boolean.TRUE.equals(membership.getBranch().getActive())
                || !Boolean.TRUE.equals(membership.getPlan().getActive())) {
            throw new IllegalStateException("Gói đăng ký không còn chờ thanh toán hoặc đã ngừng hoạt động.");
        }
        var pending = payments.findByMembership_IdAndStatus(membershipId, PaymentStatus.PENDING);
        if (!pending.isEmpty()) {
            Payment existing = pending.get(0);
            if (pending.size() != 1 || existing.getMethod() != PaymentMethod.VNPAY) {
                throw new IllegalStateException("Đã có yêu cầu thanh toán đang xử lý. Vui lòng hoàn tất tại quầy.");
            }
            return gateway.paymentUrl(existing, ipAddress);
        }
        Payment payment = Payment.builder().user(member).membership(membership)
                .amount(membership.getQuotedPrice() != null ? membership.getQuotedPrice() : membership.getPlan().getPrice())
                .method(PaymentMethod.VNPAY).status(PaymentStatus.PENDING)
                .transactionCode("UT7" + UUID.randomUUID().toString().replace("-", ""))
                .createdAt(LocalDateTime.now()).build();
        VnpayGateway.minorUnits(payment.getAmount());
        payments.saveAndFlush(payment);
        return gateway.paymentUrl(payment, ipAddress);
    }

    @Transactional
    public Map<String, String> ipn(Map<String, String> fields) {
        if (!gateway.verified(fields)) return response("97", "Invalid signature");
        Payment snapshot = payments.findByTransactionCode(fields.get("vnp_TxnRef")).orElse(null);
        if (snapshot == null || snapshot.getMethod() != PaymentMethod.VNPAY || snapshot.getMembership() == null) {
            return response("01", "Order not found");
        }
        Membership membership = memberships.lockById(snapshot.getMembership().getId()).orElseThrow();
        Payment payment = payments.lockById(snapshot.getId()).orElseThrow();
        if (payment.getUser() == null || !payment.getUser().getId().equals(membership.getUser().getId())) {
            return response("99", "Order owner mismatch");
        }
        if (!matchesAmount(payment, fields.get("vnp_Amount"))) return response("04", "Invalid amount");
        if (payment.getStatus() != PaymentStatus.PENDING) return response("02", "Order already confirmed");
        String code = fields.get("vnp_ResponseCode");
        String status = fields.get("vnp_TransactionStatus");
        String gatewayId = fields.get("vnp_TransactionNo");
        if (code == null || !code.matches("[0-9]{2}") || status == null || !status.matches("[0-9]{2}")
                || gatewayId == null || !gatewayId.matches("[0-9]{1,100}")) return response("99", "Invalid response");
        boolean paid = "00".equals(code) && "00".equals(status);
        LocalDateTime paidAt = null;
        if (paid) {
            try {
                paidAt = LocalDateTime.parse(fields.get("vnp_PayDate"), VnpayGateway.TIMESTAMP);
            } catch (RuntimeException ex) {
                return response("99", "Invalid payment date");
            }
            if (membership.getQuotedDurationDays() != null && membership.getQuotedDurationDays() <= 0) {
                return response("99", "Invalid membership duration");
            }
        }
        payment.setGatewayResponseCode(code);
        payment.setGatewayTransactionNo(gatewayId);
        payment.setStatus(paid ? PaymentStatus.PAID : PaymentStatus.FAILED);
        if (paid) {
            payment.setPaidAt(paidAt);
            if (membership.getStatus() == MembershipStatus.PENDING) billing.activateMembership(membership, paidAt);
        }
        payments.save(payment);
        return response("00", "Confirm Success");
    }

    @Transactional(readOnly = true)
    public String returnMessage(Map<String, String> fields) {
        if (!gateway.verified(fields)) return "Không thể xác minh kết quả VNPay. Vui lòng kiểm tra lịch sử thanh toán.";
        Payment payment = payments.findByTransactionCode(fields.get("vnp_TxnRef")).orElse(null);
        if (payment == null || payment.getMethod() != PaymentMethod.VNPAY
                || !matchesAmount(payment, fields.get("vnp_Amount"))) return "Không tìm thấy giao dịch phù hợp.";
        if (payment.getStatus() == PaymentStatus.PAID) {
            return payment.getMembership().getStatus() == MembershipStatus.ACTIVE
                    ? "Thanh toán đã được xác nhận. Gói tập đã kích hoạt."
                    : "Đã ghi nhận thanh toán. Vui lòng liên hệ quầy để đối soát trạng thái gói tập.";
        }
        if (payment.getStatus() == PaymentStatus.FAILED) return "Thanh toán không thành công hoặc đã bị hủy. Gói tập chưa kích hoạt.";
        return "Đang chờ xác nhận thanh toán từ VNPay. Vui lòng kiểm tra lại lịch sử sau ít phút.";
    }

    private boolean matchesAmount(Payment payment, String amount) {
        try {
            return amount != null && amount.matches("[0-9]{1,20}")
                    && new BigInteger(VnpayGateway.minorUnits(payment.getAmount())).equals(new BigInteger(amount));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Map<String, String> response(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
