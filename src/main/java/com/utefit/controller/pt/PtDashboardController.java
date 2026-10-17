package com.utefit.controller.pt;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pt")
public class PtDashboardController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "pt/dashboard";
    }
}