package com.utefit.controller.admin;

import com.utefit.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        Map<String, Long> kpis = adminDashboardService.getBasicKpiCounts();

        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("totalUsers", kpis.get("totalUsers"));
        model.addAttribute("totalBranches", kpis.get("totalBranches"));
        model.addAttribute("totalTrainers", kpis.get("totalTrainers"));
        model.addAttribute("totalClasses", kpis.get("totalClasses"));
        return "admin/dashboard";
    }
}