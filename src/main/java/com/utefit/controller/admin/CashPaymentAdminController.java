
package com.utefit.controller.admin;

import com.utefit.service.MembershipPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/payments")
public class CashPaymentAdminController {

    private final MembershipPaymentService billing;

    public CashPaymentAdminController(
            MembershipPaymentService billing
    ) {
        this.billing = billing;
    }

    @GetMapping({"", "/cash-pending"})
    public String pendingCash(Model model) {

        model.addAttribute(
                "activeMenu",
                "payments"
        );

        model.addAttribute(
                "payments",
                billing.pendingCashPayments()
        );

        return "admin/payments/cash-pending";
    }

    @PostMapping("/{id}/confirm-cash")
    public String confirmCash(
            @PathVariable Long id,
            RedirectAttributes redirect
    ) {
        try {
            billing.confirmCashPayment(id);

            redirect.addFlashAttribute(
                    "success",
                    "Đã xác nhận thu tiền. "
                            + "Membership chuyển sang ACTIVE."
            );

        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/payments";
    }
}
