
package com.utefit.controller.admin;

import com.utefit.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService service;

    @GetMapping
    public String statistics(Model model) {
        model.addAttribute("activeMenu", "statistics");
        model.addAllAttributes(service.report());
        return "admin/statistics/index";
    }
}
