package com.utefit.controller.admin;

import com.utefit.dto.admin.MembershipPlanFormDto;
import com.utefit.entity.MembershipPlan;
import com.utefit.service.AdminMembershipPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/membership-plans")
@RequiredArgsConstructor
public class AdminMembershipPlanController {

    private final AdminMembershipPlanService planService;
    private final com.utefit.repository.BranchRepository branches;

    @ModelAttribute("scopeBranches")
    public java.util.List<com.utefit.entity.Branch> scopeBranches() { return branches.findByActiveTrueOrderByNameAsc(); }

    @GetMapping
    public String listPlans(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model
    ) {
        Page<MembershipPlan> planPage = planService.getPlans(keyword, active, page, size);

        model.addAttribute("planPage", planPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedActive", active);
        model.addAttribute("activeMenu", "plans");

        return "admin/plans/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("planForm", new MembershipPlanFormDto());
        model.addAttribute("isEdit", false);
        model.addAttribute("activeMenu", "plans");
        return "admin/plans/form";
    }

    @PostMapping("/create")
    public String createPlan(
            @Valid @ModelAttribute("planForm") MembershipPlanFormDto planForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        includeSavedBranches(planForm, model);
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("activeMenu", "plans");
            return "admin/plans/form";
        }

        try {
            planService.createPlan(planForm);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo gói tập mới thành công!");
            return "redirect:/admin/membership-plans";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("activeMenu", "plans");
            return "admin/plans/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            MembershipPlanFormDto planForm = planService.getPlanFormById(id);
            includeSavedBranches(planForm, model);
            model.addAttribute("planForm", planForm);
            model.addAttribute("isEdit", true);
            model.addAttribute("activeMenu", "plans");
            return "admin/plans/form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/membership-plans";
        }
    }

    @PostMapping("/{id}/edit")
    public String updatePlan(
            @PathVariable Long id,
            @Valid @ModelAttribute("planForm") MembershipPlanFormDto planForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        planForm.setId(id);
        includeSavedBranches(planForm, model);
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("activeMenu", "plans");
            return "admin/plans/form";
        }

        try {
            planService.updatePlan(id, planForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật gói tập thành công!");
            return "redirect:/admin/membership-plans";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("activeMenu", "plans");
            return "admin/plans/form";
        }
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            planService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái gói tập!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/membership-plans";
    }

    private void includeSavedBranches(MembershipPlanFormDto form, Model model) {
        var available = new java.util.ArrayList<>(scopeBranches());
        if (form.getBranchIds() != null) {
            var selectedIds = form.getBranchIds().stream().filter(java.util.Objects::nonNull).toList();
            var activeIds = available.stream().map(com.utefit.entity.Branch::getId).collect(java.util.stream.Collectors.toSet());
            branches.findAllById(selectedIds).stream().filter(branch -> !activeIds.contains(branch.getId())).forEach(available::add);
        }
        model.addAttribute("scopeBranches", available);
    }
}
