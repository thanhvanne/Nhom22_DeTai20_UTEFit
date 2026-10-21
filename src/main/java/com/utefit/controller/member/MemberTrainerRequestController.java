package com.utefit.controller.member;

import com.utefit.service.MemberTrainerRequestService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/member/trainer-requests")
@RequiredArgsConstructor
public class MemberTrainerRequestController {

    private final MemberTrainerRequestService requests;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       Principal principal, Model model) {
        model.addAttribute("requestPage", requests.getMyRequests(principal.getName(), page));
        return "member/trainer-requests";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam Long trainerId, Model model,
                          RedirectAttributes redirect) {
        try {
            model.addAttribute("trainer", requests.getTrainer(trainerId));
            return "member/trainer-request-form";
        } catch (EntityNotFoundException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("errorMessage", "Không tìm thấy PT phù hợp.");
            return "redirect:/trainers";
        }
    }

    @PostMapping
    public String create(@RequestParam Long trainerId,
                         @RequestParam(required = false) String message,
                         Principal principal, RedirectAttributes redirect) {
        try {
            requests.sendRequest(principal.getName(), trainerId, message);
            redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu PT.");
            return "redirect:/member/trainer-requests";
        } catch (IllegalArgumentException | IllegalStateException | EntityNotFoundException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/member/trainer-requests/new?trainerId=" + trainerId;
        }
    }
}