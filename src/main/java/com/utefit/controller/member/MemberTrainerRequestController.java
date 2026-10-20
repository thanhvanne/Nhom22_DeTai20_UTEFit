package com.utefit.controller.member;

import com.utefit.dto.trainer.TrainerResponse;
import com.utefit.dto.trainerrequest.TrainerRequestResponse;
import com.utefit.entity.User;
import com.utefit.repository.UserRepository;
import com.utefit.service.TrainerRequestService;
import com.utefit.service.TrainerService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/member/trainer-requests")
@RequiredArgsConstructor
public class MemberTrainerRequestController {

    private final TrainerRequestService trainerRequestService;
    private final TrainerService trainerService;
    private final UserRepository userRepository;

    @GetMapping
    public String listMemberRequests(
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model
    ) {
        Long memberId = getCurrentUserId();

        Pageable pageable = PageRequest.of(page, 10);
        Page<TrainerRequestResponse> requestPage =
                trainerRequestService.getRequestsForMember(memberId, pageable);

        model.addAttribute("requestPage", requestPage);
        return "member/trainer-requests";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam("trainerId") Long trainerId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            TrainerResponse trainer = trainerService.getPublicTrainerById(trainerId);
            model.addAttribute("trainer", trainer);
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/trainers";
        }

        return "member/trainer-request-form";
    }

    @PostMapping
    public String createRequest(
            @RequestParam("trainerId") Long trainerId,
            @RequestParam(value = "message", required = false) String message,
            RedirectAttributes redirectAttributes
    ) {
        Long memberId = getCurrentUserId();

        try {
            trainerRequestService.createRequest(memberId, trainerId, message);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã gửi yêu cầu tới huấn luyện viên."
            );
            return "redirect:/member/trainer-requests";
        } catch (IllegalStateException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/member/trainer-requests/new?trainerId=" + trainerId;
        }
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