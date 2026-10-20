package com.utefit.controller.pt;

import com.utefit.dto.trainerrequest.TrainerRequestResponse;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import com.utefit.service.TrainerRequestService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pt/trainer-requests")
@RequiredArgsConstructor
public class PtTrainerRequestController {

    private final TrainerRequestService trainerRequestService;
    private final UserRepository userRepository;

    @GetMapping
    public String listTrainerRequests(
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model
    ) {
        Long trainerUserId = getCurrentUserId();

        Pageable pageable = PageRequest.of(page, 10);
        Page<TrainerRequestResponse> requestPage =
                trainerRequestService.getRequestsForTrainer(trainerUserId, pageable);

        model.addAttribute("requestPage", requestPage);
        return "pt/trainer-requests";
    }

    @PostMapping("/{id}/accept")
    public String acceptRequest(
            @PathVariable("id") Long requestId,
            RedirectAttributes redirectAttributes
    ) {
        Long trainerUserId = getCurrentUserId();

        try {
            trainerRequestService.acceptRequest(trainerUserId, requestId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã chấp nhận yêu cầu."
            );
        } catch (IllegalStateException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/pt/trainer-requests";
    }

    @PostMapping("/{id}/reject")
    public String rejectRequest(
            @PathVariable("id") Long requestId,
            RedirectAttributes redirectAttributes
    ) {
        Long trainerUserId = getCurrentUserId();

        try {
            trainerRequestService.rejectRequest(trainerUserId, requestId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã từ chối yêu cầu."
            );
        } catch (IllegalStateException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/pt/trainer-requests";
    }

    private Long getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Bạn cần đăng nhập để thực hiện thao tác này.");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Không tìm thấy tài khoản người dùng."));

        return user.getId();
    }
}