package com.utefit.controller.pt;

import com.utefit.dto.trainer.PtDashboardResponse;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import com.utefit.service.PtDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pt/dashboard")
public class PtDashboardController {

    private final PtDashboardService ptDashboardService;
    private final UserRepository userRepository;

    public PtDashboardController(PtDashboardService ptDashboardService,
                                 UserRepository userRepository) {
        this.ptDashboardService = ptDashboardService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản đăng nhập."));
        return user.getId();
    }

    @GetMapping
    public String viewDashboard(Authentication authentication, Model model) {
        Long userId = getCurrentUserId(authentication);
        PtDashboardResponse dashboard = ptDashboardService.getDashboardForTrainer(userId);
        model.addAttribute("dashboard", dashboard);
        return "pt/dashboard";
    }
}