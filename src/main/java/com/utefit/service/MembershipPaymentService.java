
package com.utefit.service;

import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.*;

import com.utefit.repository.BranchRepository;
import com.utefit.repository.MembershipPlanRepository;
import com.utefit.repository.MembershipRepository;
import com.utefit.repository.PaymentRepository;
import com.utefit.repository.UserRepository;
import com.utefit.enums.NotificationType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MembershipPaymentService {

    private final UserRepository users;
    private final BranchRepository branches;
    private final MembershipPlanRepository plans;
    private final MembershipRepository memberships;
    private final PaymentRepository payments;
    private final NotificationService notifications;

    public MembershipPaymentService(
            UserRepository users,
            BranchRepository branches,
            MembershipPlanRepository plans,
            MembershipRepository memberships,
            PaymentRepository payments,
            NotificationService notifications
    ) {
        this.users = users;
        this.branches = branches;
        this.plans = plans;
        this.memberships = memberships;
        this.payments = payments;
        this.notifications = notifications;
    }

    // Member chọn chi nhánh + gói.
    // Chưa thanh toán => PENDING.
    @Transactional
    public Long createMembership(
            String email,
            MembershipPurchaseRequest request
    ) {
        // Khóa theo Member để hai POST không tạo hai PENDING đồng thời.
        User user = lockedMember(email);

        Branch branch = branches.findById(
                request.getBranchId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Chi nhánh không tồn tại"
                )
        );

        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new IllegalArgumentException(
                    "Chi nhánh đang ngừng hoạt động"
            );
        }

        MembershipPlan plan = plans.findById(
                request.getPlanId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Gói tập không tồn tại"
                )
        );

        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new IllegalArgumentException(
                    "Gói tập đang ngừng kinh doanh"
            );
        }

        if (plan.getDurationDays() == null
                || plan.getDurationDays() <= 0
                || plan.getPrice() == null
                || plan.getPrice().compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            throw new IllegalArgumentException(
                    "Thông tin gói tập không hợp lệ"
            );
        }

        if (memberships.existsByUser_IdAndStatus(
                user.getId(),
                MembershipStatus.PENDING
        )) {
            throw new IllegalArgumentException(
                    "Bạn đang có gói tập chờ thanh toán"
            );
        }

        if (memberships
                .existsByUser_IdAndStatusAndEndDateGreaterThanEqual(
                        user.getId(),
                        MembershipStatus.ACTIVE,
                        LocalDate.now()
                )) {
            throw new IllegalArgumentException(
                    "Bạn đang có gói tập còn hiệu lực"
            );
        }

        Membership membership = Membership.builder()
                .user(user)
                .branch(branch)
                .plan(plan)
                .status(MembershipStatus.PENDING)
                .quotedPrice(plan.getPrice())
                .quotedDurationDays(plan.getDurationDays())
                .build();

        return memberships.save(membership).getId();
    }

    // Member tạo yêu cầu thanh toán CASH.
    // Chưa thu tiền => Payment vẫn PENDING.
    @Transactional
    public Long requestCashPayment(
            String email,
            Long membershipId
    ) {
        User member = currentMember(email);

        Membership membership = memberships
                .lockById(membershipId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy gói đăng ký"
                        )
                );

        if (!membership.getUser().getId()
                .equals(member.getId())) {
            throw new IllegalArgumentException(
                    "Bạn không sở hữu gói đăng ký này"
            );
        }

        if (membership.getStatus()
                != MembershipStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Gói này không chờ thanh toán"
            );
        }

        if (payments.existsByMembership_IdAndStatus(
                membershipId,
                PaymentStatus.PENDING
        )) {
            throw new IllegalArgumentException(
                    "Yêu cầu thanh toán đã được tạo"
            );
        }

        MembershipPlan plan = membership.getPlan();

        if (!Boolean.TRUE.equals(plan.getActive())
                || !Boolean.TRUE.equals(
                membership.getBranch().getActive()
        )) {
            throw new IllegalArgumentException(
                    "Chi nhánh hoặc gói tập đã ngừng hoạt động"
            );
        }

        Payment payment = Payment.builder()
                .user(member)
                .membership(membership)
                .amount(membership.getQuotedPrice() != null
                        ? membership.getQuotedPrice() : plan.getPrice())
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PENDING)
                .build();

        return payments.save(payment).getId();
    }

    // CHỈ ADMIN được gọi thông qua /admin/**.
    // Admin xác nhận SAU KHI đã thu tiền tại quầy.
    @Transactional
    public void confirmCashPayment(Long paymentId) {

        // Khóa Membership trước Payment, cùng thứ tự với luồng hủy đơn.
        Payment snapshot = payments.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch"));
        if (snapshot.getMembership() == null) {
            throw new IllegalArgumentException("Giao dịch không có thông tin gói tập");
        }
        Membership membership = memberships.lockById(snapshot.getMembership().getId())
                .orElseThrow(() -> new IllegalArgumentException("Gói tập không tồn tại"));
        Payment payment = payments.lockById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch"));

        if (payment.getMethod() != PaymentMethod.CASH) {
            throw new IllegalArgumentException(
                    "Chỉ xác nhận thanh toán CASH"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Giao dịch không ở trạng thái PENDING"
            );
        }

        if (payment.getMembership() == null) {
            throw new IllegalArgumentException(
                    "Giao dịch không có thông tin gói tập"
            );
        }

        if (!membership.getId().equals(payment.getMembership().getId())) {
            throw new IllegalStateException("Đơn hàng đã thay đổi trong khi thanh toán.");
        }

        if (membership.getStatus()
                != MembershipStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Gói tập không ở trạng thái PENDING"
            );
        }

        if (!payment.getUser().getId()
                .equals(membership.getUser().getId())) {
            throw new IllegalArgumentException(
                    "Chủ giao dịch không khớp chủ gói tập"
            );
        }

        if (!Boolean.TRUE.equals(
                membership.getBranch().getActive()
        ) || !Boolean.TRUE.equals(
                membership.getPlan().getActive()
        )) {
            throw new IllegalArgumentException(
                    "Chi nhánh hoặc gói tập không còn hoạt động"
            );
        }

        int duration = membership.getQuotedDurationDays() != null
                ? membership.getQuotedDurationDays() : membership.getPlan().getDurationDays();

        if (duration <= 0) {
            throw new IllegalArgumentException(
                    "Thời hạn gói tập không hợp lệ"
            );
        }

        if (payment.getAmount() == null
                || payment.getAmount().compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            throw new IllegalArgumentException(
                    "Số tiền giao dịch không hợp lệ"
            );
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionCode(
                "CASH-" + payment.getId()
        );
        activateMembership(membership, payment.getPaidAt());
        payments.save(payment);
    }

    void activateMembership(Membership membership, LocalDateTime paidAt) {
        int duration = membership.getQuotedDurationDays() != null
                ? membership.getQuotedDurationDays() : membership.getPlan().getDurationDays();
        if (duration <= 0 || membership.getStatus() != MembershipStatus.PENDING) {
            throw new IllegalStateException("Gói tập không thể kích hoạt.");
        }
        LocalDate start = paidAt.toLocalDate();
        membership.setStartDate(start);
        membership.setEndDate(start.plusDays(duration - 1L));
        membership.setStatus(MembershipStatus.ACTIVE);
        memberships.save(membership);
        notifications.createNotificationForUser(
                membership.getUser().getId(),
                NotificationType.MEMBERSHIP_ACTIVATED,
                "Gói tập đã được kích hoạt",
                "Thanh toán đã được xác nhận. Gói tập của bạn có hiệu lực đến " + membership.getEndDate() + ".",
                membership.getId());
    }

    /** Hủy đơn chưa thanh toán, đóng Payment PENDING nhưng giữ lịch sử. */
    @Transactional
    public void cancelPendingMembership(String email, Long membershipId) {
        User member = lockedMember(email);
        Membership membership = memberships.lockById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập."));
        if (!membership.getUser().getId().equals(member.getId())) {
            throw new IllegalArgumentException("Bạn không sở hữu gói này.");
        }
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new IllegalStateException("Chỉ hủy được gói đang PENDING.");
        }
        if (payments.findByMembership_IdAndStatus(membershipId, PaymentStatus.PENDING).stream()
                .anyMatch(payment -> payment.getMethod() == PaymentMethod.VNPAY || payment.getMethod() == PaymentMethod.SEPAY)) {
            throw new IllegalStateException("Giao dịch VNPay/SePay đang xử lý; cần chờ kết quả đối soát trước khi hủy gói.");
        }
        // Khóa từng payment để tuần tự với thao tác Admin xác nhận CASH.
        for (Payment payment : payments.findByMembership_IdAndStatus(
                membershipId, PaymentStatus.PENDING)) {
            Payment locked = payments.lockById(payment.getId()).orElseThrow();
            if (locked.getStatus() == PaymentStatus.PENDING) {
                locked.setStatus(PaymentStatus.FAILED);
            }
        }
        membership.setStatus(MembershipStatus.CANCELLED);
    }

    private User lockedMember(String email) {
        User user = users.lockByEmailForBooking(email)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        if (user.getRole() != UserRole.MEMBER || user.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("Tài khoản Member không hợp lệ.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<MembershipView> myMemberships(
            String email
    ) {
        User member = currentMember(email);

        return memberships
                .findByUser_IdOrderByCreatedAtDesc(
                        member.getId()
                )
                .stream()
                .map(m -> new MembershipView(
                        m.getId(),
                        m.getBranch().getName(),
                        m.getPlan().getName(),
                        m.getQuotedPrice() != null ? m.getQuotedPrice() : m.getPlan().getPrice(),
                        m.getStatus().name(),
                        m.getStartDate(),
                        m.getEndDate()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentView> myPayments(String email) {
        User member = currentMember(email);

        return payments
                .findByUser_IdOrderByCreatedAtDesc(
                        member.getId()
                )
                .stream()
                .map(p -> new PaymentView(
                        p.getId(),
                        p.getMembership() == null
                                ? "Không có"
                                : p.getMembership()
                                .getPlan().getName(),
                        p.getAmount(),
                        p.getMethod().name(),
                        p.getStatus().name(),
                        p.getTransactionCode(),
                        p.getCreatedAt(),
                        p.getPaidAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingCashView> pendingCashPayments() {

        return payments
                .findByStatusAndMethodOrderByCreatedAtAsc(
                        PaymentStatus.PENDING,
                        PaymentMethod.CASH
                )
                .stream()
                .map(p -> new PendingCashView(
                        p.getId(),
                        p.getUser().getEmail(),
                        p.getMembership().getPlan().getName(),
                        p.getMembership().getBranch().getName(),
                        p.getAmount(),
                        p.getCreatedAt()
                ))
                .toList();
    }

    private User currentMember(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản"
                        )
                );

        if (user.getRole() != UserRole.MEMBER
                || user.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException(
                    "Tài khoản không hợp lệ"
            );
        }

        return user;
    }

    public record MembershipView(
            Long id,
            String branchName,
            String planName,
            BigDecimal price,
            String status,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record PaymentView(
            Long id,
            String planName,
            BigDecimal amount,
            String method,
            String status,
            String transactionCode,
            LocalDateTime createdAt,
            LocalDateTime paidAt
    ) {}

    public record PendingCashView(
            Long id,
            String memberEmail,
            String planName,
            String branchName,
            BigDecimal amount,
            LocalDateTime createdAt
    ) {}
}
