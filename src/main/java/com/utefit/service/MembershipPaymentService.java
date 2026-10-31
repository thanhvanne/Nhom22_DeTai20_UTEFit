
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
    @org.springframework.beans.factory.annotation.Autowired private MembershipOfferService offers;
    @org.springframework.beans.factory.annotation.Autowired private PromotionReservationService promotionReservations;
    @org.springframework.beans.factory.annotation.Autowired
    private com.utefit.repository.PromotionRepository promotions;

    private final UserRepository users;
    private final BranchRepository branches;
    private final MembershipPlanRepository plans;
    private final MembershipRepository memberships;
    private final PaymentRepository payments;
    private final NotificationService notifications;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

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
        com.utefit.validation.FormChecks.validate(request);
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

        MembershipPlan plan = plans.lockForSnapshot(
                request.getPlanId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Gói tập không tồn tại"
                )
        );

        entityManager.refresh(plan);
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

        validateBranchScope(plan, branch.getId());

        Membership membership = Membership.builder()
                .user(user)
                .branch(branch)
                .plan(plan)
                .status(MembershipStatus.PENDING)
                .quotedPrice(plan.getPrice())
                .quotedDurationDays(plan.getDurationDays())
                .quotedDurationUnit(plan.term().unit()).quotedDurationQuantity(plan.term().quantity())
                .quotedBonusUnit(plan.term().bonusUnit()).quotedBonusQuantity(plan.term().bonusQuantity())
                .quotedBenefit(plan.effectiveBenefit()).quotedScope(plan.effectiveScope())
                .branchIds(new java.util.HashSet<>(plan.getBranchIds()))
                .build();

        for (Membership pending : memberships.findByUser_IdOrderByCreatedAtDesc(user.getId())) {
            if (pending.getStatus() == MembershipStatus.PENDING && pending.getPlan().getId().equals(plan.getId())
                    && pending.entitlementKey().equals(membership.entitlementKey())) {
                String requestedCode = request.getPromotionCode() == null || request.getPromotionCode().isBlank() ? null : request.getPromotionCode().trim().toUpperCase(java.util.Locale.ROOT);
                if (!java.util.Objects.equals(requestedCode, pending.getPromotionCode() == null ? null : pending.getPromotionCode().toUpperCase(java.util.Locale.ROOT))) throw new IllegalArgumentException("Đã có đơn chờ cho gói này với ưu đãi khác. Xử lý đơn cũ trước khi tạo đơn mới.");
                if (pending.getPricingSelection() != null && !pending.getPricingSelection().equals(offers.selection(request))) throw new IllegalArgumentException("Đơn chờ đã chốt lựa chọn ưu đãi. Hủy đúng quy trình trước khi đổi ưu đãi.");
                return pending.getId();
            }
        }

        offers.apply(user, membership, request);
        return memberships.save(membership).getId();
    }

    @Transactional(readOnly = true)
    public MembershipOfferService.Quote quote(String email, MembershipPurchaseRequest request) {
        User member = currentMember(email);
        Branch branch = branches.findById(request.getBranchId()).orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại."));
        MembershipPlan plan = plans.findById(request.getPlanId()).orElseThrow(() -> new IllegalArgumentException("Gói tập không tồn tại."));
        if (!Boolean.TRUE.equals(branch.getActive()) || !Boolean.TRUE.equals(plan.getActive())) throw new IllegalArgumentException("Gói hoặc chi nhánh đã ngừng hoạt động.");
        validateBranchScope(plan, branch.getId());
        var proposed = Membership.builder().branch(branch).quotedBenefit(plan.effectiveBenefit()).quotedScope(plan.effectiveScope()).branchIds(new java.util.HashSet<>(plan.getBranchIds())).build();
        for (Membership pending : memberships.findByUser_IdOrderByCreatedAtDesc(member.getId())) {
            if (pending.getStatus() != MembershipStatus.PENDING || !pending.getPlan().getId().equals(plan.getId()) || !pending.entitlementKey().equals(proposed.entitlementKey())) continue;
            String code = request.getPromotionCode() == null || request.getPromotionCode().isBlank() ? null : request.getPromotionCode().trim().toUpperCase(java.util.Locale.ROOT);
            if (!java.util.Objects.equals(code, pending.getPromotionCode() == null ? null : pending.getPromotionCode().toUpperCase(java.util.Locale.ROOT))
                    || pending.getPricingSelection() != null && !pending.getPricingSelection().equals(offers.selection(request))) throw new IllegalArgumentException("Đơn chờ đã chốt ưu đãi khác. Xử lý đơn cũ trước khi đổi ưu đãi.");
            return offers.snapshot(pending);
        }
        return offers.preview(member, plan, branch.getId(), request);
    }

    private void validateBranchScope(MembershipPlan plan, Long branchId) {
        if (!plan.fixedBranch())
            throw new IllegalArgumentException("Gói cũ chưa được cấu hình một chi nhánh cố định; liên hệ Admin. Membership đã mua vẫn giữ quyền cũ.");
        if (!plan.getBranchIds().contains(branchId)) throw new IllegalArgumentException("Gói không thuộc chi nhánh đã chọn.");
    }

    @Transactional
    public Long renew(String email, Long membershipId) {
        User user = lockedMember(email);
        Membership existing = memberships.findById(membershipId).orElseThrow(() -> new IllegalArgumentException("Không có gói tập."));
        if (!existing.getUser().getId().equals(user.getId()) || (existing.getStatus() != MembershipStatus.ACTIVE
                && existing.getStatus() != MembershipStatus.EXPIRED)) throw new IllegalArgumentException("Không thể gia hạn gói này.");
        var request = new MembershipPurchaseRequest();
        request.setPlanId(existing.getPlan().getId());
        request.setBranchId(existing.getBranch().getId());
        Long id = createMembership(email, request);
        Membership pending = memberships.findById(id).orElseThrow();
        pending.setRenewalOf(existing);
        return id;
    }

    // Member tạo yêu cầu thanh toán CASH.
    // Chưa thu tiền => Payment vẫn PENDING.
    @Transactional
    public Long requestCashPayment(
            String email,
            Long membershipId
    ) {
        User member = lockedMember(email);

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

        if (payments.existsByMembership_IdAndStatus(membershipId, PaymentStatus.CANCEL_REQUESTED))
            throw new IllegalStateException("Đơn đang yêu cầu hủy; không được tạo giao dịch mới.");
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
        User member = users.lockByIdForBooking(snapshot.getUser().getId()).orElseThrow();
        entityManager.refresh(member);
        Membership membership = memberships.lockById(snapshot.getMembership().getId())
                .orElseThrow(() -> new IllegalArgumentException("Gói tập không tồn tại"));
        Payment payment = payments.lockById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch"));
        entityManager.refresh(membership);
        entityManager.refresh(payment);
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(member.getEnabled()))
            throw new IllegalArgumentException("Tài khoản Member không hoạt động.");

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

        if (membership.getQuotedPrice() != null && membership.getQuotedPrice().compareTo(payment.getAmount()) != 0)
            throw new IllegalArgumentException("Số tiền CASH không khớp snapshot gói tập.");
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now(SepayPaymentService.ZONE));
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
        payments.flush();
        if (!payments.existsByMembership_IdAndStatus(membership.getId(), PaymentStatus.PAID))
            throw new IllegalStateException("Chỉ cộng thời hạn sau Payment PAID.");
        LocalDate start = activationStart(membership, paidAt.toLocalDate());
        membership.setStartDate(start);
        membership.setEndDate(membership.term().endDate(start));
        membership.setStatus(MembershipStatus.ACTIVE);
        memberships.save(membership);
        notifications.createNotificationForUser(
                membership.getUser().getId(),
                NotificationType.MEMBERSHIP_ACTIVATED,
                "Gói tập đã được kích hoạt",
                "Thanh toán đã được xác nhận. Gói tập của bạn có hiệu lực đến " + membership.getEndDate() + ".",
                membership.getId());
    }

    LocalDate activationStart(Membership membership, LocalDate paidDate) {
        return activationStart(membership, paidDate, memberships.findPaidAccessByUser(membership.getUser().getId()));
    }

    private LocalDate activationStart(Membership membership, LocalDate paidDate, List<Membership> paid) {
        LocalDate start = paidDate;
        LocalDate last = paid.stream()
                .filter(previous -> sharesBranchChain(previous, membership.getBranch().getId()))
                .map(Membership::getEndDate).filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        if (last != null && !last.isBefore(start)) start = last.plusDays(1);
        return start;
    }

    /** Hủy đơn chưa thanh toán, đóng Payment PENDING nhưng giữ lịch sử. */
    @Transactional
    public boolean cancelPendingMembership(String email, Long membershipId) {
        User member = lockedMember(email);
        Membership membership = memberships.lockById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập."));
        entityManager.refresh(membership);
        if (!membership.getUser().getId().equals(member.getId())) {
            throw new IllegalArgumentException("Bạn không sở hữu gói này.");
        }
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new IllegalStateException("Chỉ hủy được gói đang PENDING.");
        }
        if (payments.existsByMembership_IdAndStatus(membershipId, PaymentStatus.PAID))
            throw new IllegalStateException("Đơn đã thanh toán; không được hủy bằng luồng chưa thanh toán.");
        if (payments.existsByMembership_IdAndStatus(membershipId, PaymentStatus.CANCEL_REQUESTED)) return false;
        boolean needsApproval = payments.findByMembership_IdAndStatus(membershipId, PaymentStatus.PENDING).stream()
                .anyMatch(payment -> payment.getMethod() != PaymentMethod.CASH);
        for (Payment payment : payments.findByMembership_IdAndStatus(membershipId, PaymentStatus.PENDING)
                .stream().sorted(java.util.Comparator.comparing(Payment::getId)).toList()) {
            Payment locked = payments.lockById(payment.getId()).orElseThrow();
            entityManager.refresh(locked);
            if (locked.getStatus() == PaymentStatus.PENDING) {
                locked.setCancellationRequestedAt(LocalDateTime.now(SepayPaymentService.ZONE));
                locked.setStatus(needsApproval ? PaymentStatus.CANCEL_REQUESTED : PaymentStatus.CANCELLED);
                if (!needsApproval) {
                    locked.setCancelledAt(LocalDateTime.now(SepayPaymentService.ZONE));
                    locked.setCancelledBy(member.getEmail());
                    locked.setCancellationReason("Hội viên hủy yêu cầu tiền mặt chưa được xác nhận thu tiền.");
                }
            }
        }
        if (needsApproval) return false;
        if (membership.getPromotionCode() != null) promotionReservations.release(membership);
        membership.setStatus(MembershipStatus.CANCELLED);
        return true;
    }

    @Transactional
    public void approvePendingCancellation(String adminEmail, Long paymentId, String reason, boolean reviewed) {
        User actor = users.findByEmail(adminEmail).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Admin không tồn tại."));
        entityManager.refresh(actor);
        if (actor.getRole() != UserRole.ADMIN || actor.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(actor.getEnabled()))
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Admin đang hoạt động được xử lý yêu cầu hủy.");
        if (!reviewed || reason == null || reason.trim().length() < 10 || reason.trim().length() > 500)
            throw new IllegalArgumentException("Cần kiểm tra lịch sử thu tiền và ghi lý do duyệt hủy (10–500 ký tự).");
        Payment snapshot = payments.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch."));
        if (snapshot.getMembership() == null) throw new IllegalArgumentException("Giao dịch không có Membership.");
        User member = users.lockByIdForBooking(snapshot.getUser().getId()).orElseThrow();
        entityManager.refresh(member);
        Membership membership = memberships.lockById(snapshot.getMembership().getId()).orElseThrow();
        entityManager.refresh(membership);
        if (membership.getStatus() != MembershipStatus.PENDING || payments.existsByMembership_IdAndStatus(membership.getId(), PaymentStatus.PAID))
            throw new IllegalStateException("Gói không còn chờ thanh toán hoặc đã có tiền được xác nhận. Không thể duyệt hủy.");
        var requests = payments.findByMembership_IdAndStatus(membership.getId(), PaymentStatus.CANCEL_REQUESTED);
        if (requests.stream().noneMatch(payment -> payment.getId().equals(paymentId)))
            throw new IllegalStateException("Chưa có yêu cầu hủy đang chờ xử lý cho giao dịch này.");
        for (Payment request : requests.stream().sorted(java.util.Comparator.comparing(Payment::getId)).toList()) {
            Payment locked = payments.lockById(request.getId()).orElseThrow();
            entityManager.refresh(locked);
            if (locked.getStatus() != PaymentStatus.CANCEL_REQUESTED) throw new IllegalStateException("Trạng thái giao dịch đã thay đổi.");
            locked.setStatus(PaymentStatus.CANCELLED);
            locked.setCancelledAt(LocalDateTime.now(SepayPaymentService.ZONE));
            locked.setCancelledBy(actor.getEmail());
            locked.setCancellationReason(reason.trim());
        }
        if (membership.getPromotionCode() != null) promotionReservations.release(membership);
        membership.setStatus(MembershipStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public java.util.Set<Long> cancellationMembershipIds(String email) {
        User member = currentMember(email);
        return payments.findByUser_IdOrderByCreatedAtDesc(member.getId()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.CANCEL_REQUESTED && payment.getMembership() != null)
                .map(payment -> payment.getMembership().getId()).collect(java.util.stream.Collectors.toSet());
    }

    private User lockedMember(String email) {
        User user = users.lockByEmailForBooking(email)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        entityManager.refresh(user);
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

        List<Membership> paid = memberships.findPaidAccessByUser(member.getId());
        var memberPayments = payments.findByUser_IdOrderByCreatedAtDesc(member.getId());
        var confirmed = memberPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PAID && payment.getMembership() != null)
                .map(payment -> payment.getMembership().getId()).collect(java.util.stream.Collectors.toSet());
        var ordersWithPayment = memberPayments.stream().filter(payment -> payment.getMembership() != null)
                .map(payment -> payment.getMembership().getId()).collect(java.util.stream.Collectors.toSet());
        return memberships
                .findByUser_IdOrderByCreatedAtDesc(
                        member.getId()
                )
                .stream()
                .map(m -> membershipView(m, paid, confirmed.contains(m.getId()) || (m.getQuotedScope() == null && !ordersWithPayment.contains(m.getId()))))
                .toList();
    }

    private MembershipView membershipView(Membership m, List<Membership> paid, boolean paymentConfirmed) {
        LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        boolean usable = paid.stream().anyMatch(period -> period.getId().equals(m.getId()));
        String periodState = m.getStatus().name();
        if (usable && m.getStartDate() != null && m.getEndDate() != null)
            periodState = m.getStartDate().isAfter(today) ? "FUTURE" : m.getEndDate().isBefore(today) ? "EXPIRED" : "CURRENT";
        LocalDate projectedStart = activationStart(m, today, paid);
        var projectedTerm = m.getStatus() == MembershipStatus.PENDING ? m.term() : m.getPlan().term();
        return new MembershipView(
                        m.getId(),
                        m.getBranch().getName(),
                        m.getPlan().getName(),
                        m.getQuotedPrice() != null ? m.getQuotedPrice() : m.getPlan().getPrice(),
                        m.getStatus().name(),
                        m.getStartDate(),
                        m.getEndDate(), m.effectiveBenefit().name(), m.effectiveScope().name(),
                        java.util.Set.copyOf(m.getBranchIds()),
                        (m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.EXPIRED)
                                && Boolean.TRUE.equals(m.getPlan().getActive()) && m.getPlan().fixedBranch()
                                && m.getPlan().getBranchIds().contains(m.getBranch().getId()),
                        projectedTerm.daysAt(projectedStart), projectedTerm.endDate(projectedStart),
                        paidThrough(m, paid), m.getPlan().getPrice(), m.getBranch().getId(), periodState,
                        "CURRENT".equals(periodState) ? java.time.temporal.ChronoUnit.DAYS.between(today, m.getEndDate()) + 1 : 0,
                        m.term().description(), projectedStart, projectedTerm.description(), paymentConfirmed, m.getPromotionCode(), m.getOriginalPrice(), m.getDiscountPercent(), m.getOfferLabel(), m.getDiscountAmount()
                );
    }

    @Transactional(readOnly = true)
    public List<BranchTimeline> branchTimelines(String email) {
        List<MembershipView> views = myMemberships(email);
        var grouped = views.stream().collect(java.util.stream.Collectors.groupingBy(MembershipView::branchId, java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));
        return grouped.entrySet().stream().map(entry -> {
            var periods = entry.getValue().stream().sorted(java.util.Comparator
                    .comparingInt((MembershipView period) -> switch (period.periodState()) { case "CURRENT" -> 0; case "FUTURE" -> 1; case "PENDING" -> 2; default -> 3; })
                    .thenComparing(MembershipView::startDate, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))) .toList();
            LocalDate end = periods.stream().filter(MembershipView::paymentConfirmed)
                    .filter(period -> period.status().equals("ACTIVE") || period.status().equals("EXPIRED"))
                    .map(MembershipView::endDate).filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
            LocalDate continuous = periods.get(0).paidThrough();
            return new BranchTimeline(entry.getKey(), periods.get(0).branchName(), end, continuous,
                    end != null && !end.isBefore(LocalDate.now(SepayPaymentService.ZONE)) && (continuous == null || end.isAfter(continuous)), periods);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOption> purchaseOptions(String email) {
        User member = currentMember(email);
        var paid = memberships.findPaidAccessByUser(member.getId());
        var activeBranches = branches.findByActiveTrueOrderByNameAsc().stream().collect(java.util.stream.Collectors.toMap(Branch::getId, branch -> branch));
        return plans.findByActiveTrueOrderByNameAsc().stream().filter(MembershipPlan::fixedBranch).filter(plan -> activeBranches.containsKey(plan.getBranchIds().iterator().next()))
                .map(plan -> {
                    Branch branch = activeBranches.get(plan.getBranchIds().iterator().next());
                    var proposed = Membership.builder().user(member).branch(branch).build();
                    LocalDate start = activationStart(proposed, LocalDate.now(SepayPaymentService.ZONE), paid);
                    return new PurchaseOption(plan.getId(), branch.getId(), branch.getName(), plan.getName(), plan.getPrice(),
                            plan.effectiveBenefit().name(), plan.term().description(), start, plan.term().endDate(start));
                }).sorted(java.util.Comparator.comparing(PurchaseOption::branchName).thenComparing(PurchaseOption::planName)).toList();
    }

    public record PurchaseOption(Long planId, Long branchId, String branchName, String planName, BigDecimal price,
                                 String benefit, String durationDescription, LocalDate projectedStart, LocalDate projectedEnd) {}
    public record BranchTimeline(Long branchId, String branchName, LocalDate totalEnd, LocalDate continuousThrough, boolean gap, List<MembershipView> periods) {}

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
            LocalDate endDate, String benefit, String accessScope, java.util.Set<Long> branchIds,
            boolean renewable, Integer renewalDays, LocalDate projectedEnd, LocalDate paidThrough, BigDecimal renewalPrice,
            Long branchId, String periodState, long daysRemaining, String durationDescription, LocalDate projectedStart, String renewalDescription, boolean paymentConfirmed, String promotionCode, BigDecimal originalPrice, BigDecimal discountPercent, String offerLabel, BigDecimal discountAmount
    ) {}

    private LocalDate paidThrough(Membership membership, List<Membership> paid) {
        LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        LocalDate end = today.minusDays(1);
        for (Membership period : paid.stream().filter(candidate -> sharesBranchChain(candidate, membership.getBranch().getId()))
                .filter(candidate -> candidate.getStartDate() != null && candidate.getEndDate() != null)
                .sorted(java.util.Comparator.comparing(Membership::getStartDate)).toList()) {
            if (!period.getStartDate().isAfter(end.plusDays(1)) && period.getEndDate().isAfter(end)) end = period.getEndDate();
        }
        return end.isBefore(today) ? null : end;
    }

    private boolean sharesBranchChain(Membership previous, Long branchId) {
        return previous.getBranch().getId().equals(branchId)
                || (previous.effectiveScope() != MembershipScope.SINGLE_BRANCH && MembershipEligibilityService.allowsBranch(previous, branchId));
    }

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
