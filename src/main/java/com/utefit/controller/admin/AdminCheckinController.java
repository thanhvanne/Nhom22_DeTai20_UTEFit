
package com.utefit.controller.admin;

import com.utefit.service.AdminCheckinService;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/checkins")
@RequiredArgsConstructor
public class AdminCheckinController {

    private final AdminCheckinService service;

    @GetMapping
    public String index(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false) Boolean open,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        model.addAttribute("activeMenu", "checkins");
        model.addAttribute("branches", service.branches());
        model.addAttribute("checkinPage",
                service.history(email, branchId, date, open, page));
        model.addAttribute("email", email);
        model.addAttribute("branchId", branchId);
        model.addAttribute("date", date);
        model.addAttribute("open", open);
        return "admin/checkins/list";
    }

    @PostMapping
    public String checkIn(
            @RequestParam String email,
            @RequestParam Long branchId,
            RedirectAttributes ra
    ) {
        try {
            service.checkIn(email, branchId);
            ra.addFlashAttribute("success",
                    "Check-in cho Member thành công.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/checkins";
    }

    @PostMapping("/{id}/checkout")
    public String checkOut(
            @PathVariable Long id,
            RedirectAttributes ra
    ) {
        try {
            service.checkOut(id);
            ra.addFlashAttribute("success",
                    "Checkout thành công.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/checkins";
    }
}
