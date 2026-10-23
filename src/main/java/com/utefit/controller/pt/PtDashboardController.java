package com.utefit.controller.pt;

import com.utefit.service.CurrentAccountService;
import com.utefit.service.PtDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

@Controller
@RequestMapping("/pt")
@RequiredArgsConstructor
public class PtDashboardController {
    private final PtDashboardService dashboards;
    private final CurrentAccountService accounts;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("dashboard", dashboards.getDashboardForTrainer(
                accounts.requirePtUserId(principal.getName())));
        return "pt/dashboard";
    }
}
