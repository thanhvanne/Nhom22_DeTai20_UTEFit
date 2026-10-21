
package com.utefit.controller.admin;

import com.utefit.service.AdminDashboardService;
import com.utefit.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminStatisticsService adminStatisticsService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {

        var kpis = adminDashboardService.getBasicKpiCounts();

        model.addAttribute("totalUsers", kpis.get("totalUsers"));
        model.addAttribute("totalBranches", kpis.get("totalBranches"));
        model.addAttribute("totalTrainers", kpis.get("totalTrainers"));
        model.addAttribute("totalClasses", kpis.get("totalClasses"));

        model.addAllAttributes(adminStatisticsService.report());
        model.addAttribute("activeMenu", "dashboard");

        return "admin/dashboard";
    }
}
