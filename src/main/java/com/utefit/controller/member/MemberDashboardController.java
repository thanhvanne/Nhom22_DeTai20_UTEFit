package com.utefit.controller.member;

import com.utefit.service.MemberDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MemberDashboardController {

    private final MemberDashboardService dashboardService;

    @GetMapping("/member/dashboard")
    public String dashboard(
            Principal principal,
            Model model
    ) {

        model.addAttribute(
                "dashboard",
                dashboardService.getDashboard(
                        principal.getName()
                )
        );

        return "member/dashboard";
    }
}