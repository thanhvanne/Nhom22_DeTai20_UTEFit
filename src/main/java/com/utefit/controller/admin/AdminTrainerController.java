package com.utefit.controller.admin;

import com.utefit.entity.Trainer;
import com.utefit.service.AdminTrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/trainers")
@RequiredArgsConstructor
public class AdminTrainerController {

    private final AdminTrainerService adminTrainerService;

    @GetMapping
    public String listTrainers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model
    ) {
        Page<Trainer> trainerPage = adminTrainerService.getTrainers(keyword, branchId, active, page, size);

        model.addAttribute("trainerPage", trainerPage);
        model.addAttribute("branches", adminTrainerService.getAllBranches());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedActive", active);
        model.addAttribute("activeMenu", "trainers");

        return "admin/trainers/list";
    }

    @PostMapping("/{id}/assign-branch")
    public String assignBranch(
            @PathVariable Long id,
            @RequestParam(required = false) Long branchId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminTrainerService.assignBranch(id, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chi nhánh làm việc cho PT thành công!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/trainers";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminTrainerService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái hoạt động của PT!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/trainers";
    }
}