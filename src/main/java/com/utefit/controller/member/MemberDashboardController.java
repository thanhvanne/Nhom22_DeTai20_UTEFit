package com.utefit.controller.member;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MemberDashboardController {

    @GetMapping("/member/dashboard")
    public String dashboard() {

        return "member/dashboard";
    }
}