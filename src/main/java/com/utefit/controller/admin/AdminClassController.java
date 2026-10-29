package com.utefit.controller.admin;

import com.utefit.dto.admin.FitnessClassFormDto;
import com.utefit.entity.FitnessClass;
import com.utefit.enums.ClassStatus;
import com.utefit.service.AdminClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/classes")
@RequiredArgsConstructor
public class AdminClassController {

    private final AdminClassService adminClassService;

    @GetMapping
    public String listClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long trainerId,
            @RequestParam(required = false) ClassStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model
    ) {
        Page<FitnessClass> classPage = adminClassService.getClasses(
                keyword, branchId, categoryId, trainerId, status, page, size
        );

        model.addAttribute("classPage", classPage);
        model.addAttribute("branches", adminClassService.getActiveBranches());
        model.addAttribute("categories", adminClassService.getActiveCategories());
        model.addAttribute("trainers", adminClassService.getActiveTrainers());
        model.addAttribute("statuses", ClassStatus.values());

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedTrainerId", trainerId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeMenu", "classes");

        return "admin/classes/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("classForm", new FitnessClassFormDto());
        populateFormOptions(model, false);
        return "admin/classes/form";
    }

    @PostMapping("/create")
    public String createClass(
            @Valid @ModelAttribute("classForm") FitnessClassFormDto classForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormOptions(model, false);
            return "admin/classes/form";
        }

        try {
            adminClassService.createClass(classForm);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo lớp tập mới thành công!");
            return "redirect:/admin/classes";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormOptions(model, false);
            return "admin/classes/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            FitnessClassFormDto classForm = adminClassService.getClassFormById(id);
            model.addAttribute("classForm", classForm);
            populateFormOptions(model, true);
            return "admin/classes/form";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/classes";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateClass(
            @PathVariable Long id,
            @Valid @ModelAttribute("classForm") FitnessClassFormDto classForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        classForm.setId(id);
        if (bindingResult.hasErrors()) {
            populateFormOptions(model, true);
            return "admin/classes/form";
        }

        try {
            adminClassService.updateClass(id, classForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin lớp tập thành công!");
            return "redirect:/admin/classes";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormOptions(model, true);
            return "admin/classes/form";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelClass(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminClassService.cancelClass(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã chuyển lớp tập sang trạng thái HỦY (CANCELLED) và đóng các lịch học liên quan!"
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/classes";
    }

    private void populateFormOptions(Model model, boolean isEdit) {
        model.addAttribute("branches", adminClassService.getActiveBranches());
        model.addAttribute("categories", adminClassService.getActiveCategories());
        model.addAttribute("trainers", adminClassService.getActiveTrainers());
        model.addAttribute("statuses", ClassStatus.values());
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("activeMenu", "classes");
    }
}