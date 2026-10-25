package com.utefit.controller.member;

import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.service.MembershipPaymentService;
import com.utefit.service.PublicCatalogService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/member")
public class MemberMembershipController {

    private final MembershipPaymentService billing;
    private final PublicCatalogService catalog;

    public MemberMembershipController(
            MembershipPaymentService billing,
            PublicCatalogService catalog
    ) {
        this.billing = billing;
        this.catalog = catalog;
    }

    @GetMapping("/memberships")
    public String memberships(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long planId,
            Principal principal,
            Model model
    ) {
        MembershipPurchaseRequest form =
                new MembershipPurchaseRequest();

        // Có chọn gói từ trang trước thì chọn sẵn gói đó.
        if (planId != null && planId > 0) {
            form.setPlanId(planId);
        }

        // Có chọn chi nhánh từ trang trước thì chọn sẵn chi nhánh đó.
        if (branchId != null && branchId > 0) {
            form.setBranchId(branchId);
        }

        model.addAttribute("purchaseForm", form);

        prepareMembershipPage(principal, model);

        return "member/memberships";
    }

    @PostMapping("/memberships")
    public String purchase(
            @Valid @ModelAttribute("purchaseForm")
            MembershipPurchaseRequest form,
            BindingResult result,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            prepareMembershipPage(principal, model);
            return "member/memberships";
        }

        try {
            billing.createMembership(
                    principal.getName(),
                    form
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đã tạo đăng ký gói tập PENDING. "
                            + "Hãy chọn CASH, VNPay Sandbox hoặc chuyển khoản VietQR (SePay)."
            );
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/member/memberships";
    }

    @PostMapping("/memberships/{id}/cash")
    public String requestCash(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            billing.requestCashPayment(
                    principal.getName(),
                    id
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đã tạo yêu cầu thanh toán CASH. "
                            + "Vui lòng thanh toán tại quầy "
                            + "và chờ Admin xác nhận."
            );
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/member/memberships";
    }

    @PostMapping("/memberships/{id}/cancel")
    public String cancelPending(@PathVariable Long id, Principal principal,
                                RedirectAttributes redirect) {
        try {
            billing.cancelPendingMembership(principal.getName(), id);
            redirect.addFlashAttribute("success", "Đã hủy gói đang chờ thanh toán.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/member/memberships";
    }

    @GetMapping("/payments")
    public String paymentHistory(
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "payments",
                billing.myPayments(principal.getName())
        );

        return "member/payments";
    }

    private void prepareMembershipPage(
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "memberships",
                billing.myMemberships(principal.getName())
        );

        model.addAttribute(
                "branches",
                catalog.activeBranchOptions()
        );

        model.addAttribute(
                "plans",
                catalog.activePlanOptions()
        );

        if (!model.containsAttribute("purchaseForm")) {
            model.addAttribute(
                    "purchaseForm",
                    new MembershipPurchaseRequest()
            );
        }
    }
}
