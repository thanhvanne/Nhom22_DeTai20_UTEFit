
package com.utefit.controller.admin;

import com.utefit.enums.PaymentMethod;
import com.utefit.enums.PaymentStatus;
import com.utefit.service.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService service;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute("activeMenu", "payments");
        model.addAttribute("paymentPage",
                service.search(keyword, method, status, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedMethod", method);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("statuses", PaymentStatus.values());
        return "admin/payments/list";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            model.addAttribute("activeMenu", "payments");
            model.addAttribute("payment", service.detail(id));
            return "admin/payments/detail";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/payments";
        }
    }
}
