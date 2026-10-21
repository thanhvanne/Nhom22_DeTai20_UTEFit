
package com.utefit.controller.admin;

import com.utefit.service.MembershipPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class CashPaymentAdminController {

    private final MembershipPaymentService billing;

    @GetMapping("/cash-pending")
    public String pendingCash(Model model) {
        model.addAttribute("activeMenu", "payments");
        model.addAttribute("payments", billing.pendingCashPayments());
        return "admin/payments/cash-pending";
    }

    @PostMapping("/{id}/confirm-cash")
    public String confirmCash(
            @PathVariable Long id,
            RedirectAttributes ra
    ) {
        try {
            billing.confirmCashPayment(id);
            ra.addFlashAttribute("success",
                    "Đã xác nhận thu tiền. Membership chuyển sang ACTIVE.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/payments/cash-pending";
    }
}
