
package com.utefit.service;

import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.*;

import com.utefit.repository.BranchRepository;
import com.utefit.repository.MembershipPlanRepository;
import com.utefit.repository.MembershipRepository;
import com.utefit.repository.PaymentRepository;
import com.utefit.repository.UserRepository;

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

    public MembershipPaymentService(
            UserRepository users,
            BranchRepository branches,
            MembershipPlanRepository plans,
            MembershipRepository memberships,
            PaymentRepository payments
    ) {
        this.users = users;
        this.branches = branches;
        this.plans = plans;
        this.memberships = memberships;
        this.payments = payments;
    }

    // Member chọn chi nhánh + gói.
    // Chưa thanh toán => PENDING.
    @Transactional
    public Long createMembership(
            String email,
            MembershipPurchaseRequest request
    ) {
        User user = currentMember(email);

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
                .amount(plan.getPrice())
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PENDING)
                .build();

        return payments.save(payment).getId();
    }

    // CHỈ ADMIN được gọi thông qua /admin/**.
    // Admin xác nhận SAU KHI đã thu tiền tại quầy.
    @Transactional
    public void confirmCashPayment(Long paymentId) {

        Payment payment = payments.lockById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy giao dịch"
                        )
                );

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

        Membership membership = memberships
                .lockById(payment.getMembership().getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Gói tập không tồn tại"
                        )
                );

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

        int duration = membership.getPlan()
                .getDurationDays();

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

        LocalDate start = LocalDate.now();

        membership.setStartDate(start);
        membership.setEndDate(start.plusDays(duration));
        membership.setStatus(MembershipStatus.ACTIVE);

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionCode(
                "CASH-" + payment.getId()
        );

        memberships.save(membership);
        payments.save(payment);
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
                        m.getPlan().getPrice(),
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
