package com.utefit.controller.pt;

import com.utefit.dto.trainer.TrainerDashboardSummaryResponse;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import com.utefit.service.PtDashboardService;
import jakarta.persistence.EntityNotFoundException;
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

    private final PtDashboardService ptDashboardService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        Long trainerUserId = getCurrentUserId(principal);

        // Lấy summary từ service
        TrainerDashboardSummaryResponse dashboard =
                ptDashboardService.getDashboardForTrainer(trainerUserId);

        // Đặt đúng tên attribute mà view đang dùng: "dashboard"
        model.addAttribute("dashboard", dashboard);

        // Nếu sau này anh muốn dùng thêm summary.* khác, có thể add thêm attribute "summary"
        // model.addAttribute("summary", dashboard);

        return "pt/dashboard";
    }

    private Long getCurrentUserId(Principal principal) {
        if (principal == null) {
            throw new EntityNotFoundException("Không tìm thấy người dùng đăng nhập.");
        }
        String email = principal.getName(); // username = email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy người dùng với email: " + email));
        return user.getId();
    }
}