package com.utefit.controller.admin;

import com.utefit.dto.admin.BranchFormDto;
import com.utefit.entity.Branch;
import com.utefit.service.AdminBranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/branches")
@RequiredArgsConstructor
public class AdminBranchController {

    private final AdminBranchService adminBranchService;

    @GetMapping
    public String listBranches(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model
    ) {
        Page<Branch> branchPage = adminBranchService.getBranches(keyword, active, page, size);

        model.addAttribute("activeMenu", "branches");
        model.addAttribute("branchPage", branchPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedActive", active);
        return "admin/branches/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("activeMenu", "branches");
        model.addAttribute("branchForm", new BranchFormDto());
        model.addAttribute("isEdit", false);
        return "admin/branches/form";
    }

    @PostMapping("/create")
    public String createBranch(
            @Valid @ModelAttribute("branchForm") BranchFormDto branchForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "branches");
            model.addAttribute("isEdit", false);
            return "admin/branches/form";
        }
        try {
            adminBranchService.createBranch(branchForm);
            ra.addFlashAttribute("successMessage", "Thêm chi nhánh mới thành công!");
            return "redirect:/admin/branches";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("activeMenu", "branches");
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/branches/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            BranchFormDto form = adminBranchService.getBranchFormById(id);
            model.addAttribute("activeMenu", "branches");
            model.addAttribute("branchForm", form);
            model.addAttribute("isEdit", true);
            return "admin/branches/form";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/branches";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateBranch(
            @PathVariable Long id,
            @Valid @ModelAttribute("branchForm") BranchFormDto branchForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "branches");
            model.addAttribute("isEdit", true);
            return "admin/branches/form";
        }
        try {
            adminBranchService.updateBranch(id, branchForm);
            ra.addFlashAttribute("successMessage", "Cập nhật chi nhánh thành công!");
            return "redirect:/admin/branches";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("activeMenu", "branches");
            model.addAttribute("isEdit", true);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/branches/form";
        }
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminBranchService.toggleActive(id);
            ra.addFlashAttribute("successMessage", "Đã đổi trạng thái hoạt động của chi nhánh!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/branches";
    }
}