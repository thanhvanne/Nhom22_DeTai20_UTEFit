package com.utefit.controller.pt;

import com.utefit.service.CurrentAccountService;
import com.utefit.service.TrainerRequestService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequestMapping("/pt/trainer-requests")
@RequiredArgsConstructor
public class PtTrainerRequestController {
    private final TrainerRequestService requests;
    private final CurrentAccountService accounts;

    @GetMapping
    public String listTrainerRequests(@RequestParam(defaultValue="0") int page,
                                      Model model, Principal principal) {
        model.addAttribute("requestPage",requests.getRequestsForTrainer(
                accounts.requirePtUserId(principal.getName()),
                PageRequest.of(Math.max(page,0),10)));
        return "pt/trainer-requests";
    }
    @PostMapping("/{id}/accept")
    public String acceptRequest(@PathVariable Long id, Principal principal,RedirectAttributes ra) {
        try {
            requests.acceptRequest(accounts.requirePtUserId(principal.getName()),id);
            ra.addFlashAttribute("successMessage","Đã chấp nhận yêu cầu.");
        } catch (IllegalStateException|EntityNotFoundException ex) {
            ra.addFlashAttribute("errorMessage",ex.getMessage());
        }
        return "redirect:/pt/trainer-requests";
    }
    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable Long id, Principal principal,RedirectAttributes ra) {
        try {
            requests.rejectRequest(accounts.requirePtUserId(principal.getName()),id);
            ra.addFlashAttribute("successMessage","Đã từ chối yêu cầu.");
        } catch (IllegalStateException|EntityNotFoundException ex) {
            ra.addFlashAttribute("errorMessage",ex.getMessage());
        }
        return "redirect:/pt/trainer-requests";
    }
}
