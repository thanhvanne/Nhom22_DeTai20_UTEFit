package com.utefit.controller.admin;

import com.utefit.service.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class SepayReconciliationController {
    private final SepayWebhookService webhooks;

    @GetMapping("/admin/payments/sepay-reconciliation")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("transactions", webhooks.reconciliation(page));
        model.addAttribute("activeMenu", "payments");
        return "admin/payments/sepay-reconciliation";
    }
}
